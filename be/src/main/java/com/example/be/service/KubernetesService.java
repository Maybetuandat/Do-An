package com.example.be.service;

import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.be.dto.CreateLabRequest;
import com.example.be.dto.CommandResultResponse;
import com.example.be.model.LabTemplate;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class KubernetesService {

    private CoreV1Api api;
    private ApiClient client;
    private static final String NAMESPACE = "default";

    @PostConstruct
    public void init() throws Exception {
        client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        api = new CoreV1Api();
        log.info("Kubernetes client initialized");
    }

    public String createLabPod(String labId, CreateLabRequest request) throws Exception {
        V1Pod pod = buildLabPod(labId, request);
        V1Pod createdPod = api.createNamespacedPod(NAMESPACE, pod, null, null, null, null);
        
        String podName = createdPod.getMetadata().getName();
        log.info("Pod created: {}", podName);
        return podName;
    }

    public String createLabPodFromTemplate(String labId, LabTemplate template) throws Exception {
        V1Pod pod = buildLabPodFromTemplate(labId, template);
        V1Pod createdPod = api.createNamespacedPod(NAMESPACE, pod, null, null, null, null);
        
        String podName = createdPod.getMetadata().getName();
        log.info("Pod created from template: {} for lab: {}", template.getName(), podName);
        return podName;
    }

    public void deletePod(String podName) throws Exception {
        api.deleteNamespacedPod(podName, NAMESPACE, null, null, null, null, null, null);
        log.info("Pod deleted: {}", podName);
    }

    public String getPodStatus(String podName) throws Exception {
        V1Pod pod = api.readNamespacedPod(podName, NAMESPACE, null);
        String phase = pod.getStatus().getPhase();
        
        // Convert Kubernetes phase to user-friendly status
        switch (phase) {
            case "Pending":
                return "Creating";
            case "Running":
                return "Running";
            case "Succeeded":
            case "Failed":
                return "Stopped";
            default:
                return "Unknown";
        }
    }

    public CommandResultResponse executeCommand(String podName, String command) throws Exception {
        return executeCommand(podName, command, 30, "/");
    }

    public CommandResultResponse executeSetupCommand(String podName, String command, int timeoutSeconds, String workingDirectory) throws Exception {
        return executeCommand(podName, command, timeoutSeconds, workingDirectory);
    }

    private CommandResultResponse executeCommand(String podName, String command, int timeoutSeconds, String workingDirectory) throws Exception {
        log.info("Executing command '{}' in pod '{}' with timeout {}s", command, podName, timeoutSeconds);
        
        // Check if pod is running
        V1Pod pod = api.readNamespacedPod(podName, NAMESPACE, null);
        if (!"Running".equals(pod.getStatus().getPhase())) {
            return CommandResultResponse.builder()
                    .command(command)
                    .output("")
                    .error("Pod is not in running state. Current status: " + pod.getStatus().getPhase())
                    .exitCode(-1)
                    .success(false)
                    .build();
        }

        try {
            Exec exec = new Exec();
            
            // Build command with working directory change if needed
            String fullCommand = command;
            if (workingDirectory != null && !workingDirectory.equals("/")) {
                fullCommand = "cd " + workingDirectory + " && " + command;
            }
            
            String[] commandParts = {"/bin/sh", "-c", fullCommand};
            
            // Create output streams to capture result
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            
            // Execute command with proper stream handling
            Process proc = exec.exec(
                NAMESPACE, 
                podName, 
                commandParts, 
                "lab-container",  // container name
                false,  // stdin
                false   // tty - set to false to avoid WebSocket issues
            );

            // Use separate threads to read streams to avoid blocking
            Thread stdoutThread = new Thread(() -> {
                try {
                    if (proc.getInputStream() != null) {
                        proc.getInputStream().transferTo(stdout);
                    }
                } catch (IOException e) {
                    log.warn("Error reading stdout: {}", e.getMessage());
                }
            });

            Thread stderrThread = new Thread(() -> {
                try {
                    if (proc.getErrorStream() != null) {
                        proc.getErrorStream().transferTo(stderr);
                    }
                } catch (IOException e) {
                    log.warn("Error reading stderr: {}", e.getMessage());
                }
            });

            stdoutThread.start();
            stderrThread.start();
            
            // Wait for completion with timeout
            boolean finished = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            // Wait for stream reading to complete
            stdoutThread.join(5000);
            stderrThread.join(5000);
            
            int exitCode = finished ? proc.exitValue() : -1;
            String output = stdout.toString("UTF-8");
            String error = stderr.toString("UTF-8");
            
            if (!finished) {
                proc.destroyForcibly();
                error = "Command timed out after " + timeoutSeconds + " seconds";
                exitCode = -1;
            }
            
            log.info("Command executed. Exit code: {}, Output length: {}, Error length: {}", 
                    exitCode, output.length(), error.length());
            
            return CommandResultResponse.builder()
                    .command(command)
                    .output(output)
                    .error(error)
                    .exitCode(exitCode)
                    .success(exitCode == 0)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to execute command in pod {}: {}", podName, e.getMessage(), e);
            return CommandResultResponse.builder()
                    .command(command)
                    .output("")
                    .error("Failed to execute command: " + e.getMessage())
                    .exitCode(-1)
                    .success(false)
                    .build();
        }
    }

    private V1Pod buildLabPod(String labId, CreateLabRequest request) {
        Map<String, String> labImages = getLabImages();
        String image = labImages.get(request.getLabType());
        
        if (image == null) {
            throw new IllegalArgumentException("Unsupported lab type: " + request.getLabType());
        }

        V1Container container = new V1Container()
                .name("lab-container")
                .image(image)
                .command(getContainerCommand(request.getLabType()))
                .env(List.of(
                        new V1EnvVar().name("LAB_TYPE").value(request.getLabType()),
                        new V1EnvVar().name("USER_ID").value(request.getUserId()),
                        new V1EnvVar().name("LAB_ID").value(labId)
                ));

        // Add volume mounts for docker labs
        if ("docker".equals(request.getLabType())) {
            container.addVolumeMountsItem(new V1VolumeMount()
                    .name("docker-sock")
                    .mountPath("/var/run/docker.sock"));
        }

        V1PodSpec spec = new V1PodSpec()
                .containers(List.of(container))
                .restartPolicy("Never")
                .activeDeadlineSeconds((long) request.getDuration());

        // Add volumes for docker labs
        if ("docker".equals(request.getLabType())) {
            spec.addVolumesItem(new V1Volume()
                    .name("docker-sock")
                    .hostPath(new V1HostPathVolumeSource().path("/var/run/docker.sock")));
        }

        Map<String, String> labels = new HashMap<>();
        labels.put("app", "lab");
        labels.put("userId", request.getUserId());
        labels.put("labType", request.getLabType());
        labels.put("labId", labId);

        return new V1Pod()
                .metadata(new V1ObjectMeta()
                        .name(labId)
                        .labels(labels))
                .spec(spec);
    }

   

private V1Pod buildLabPodFromTemplate(String labId, LabTemplate template) {
    V1Container container = new V1Container()
            .name("lab-container")
            .image(template.getBaseImage())
            .command(getTemplateContainerCommand(template.getLabType()))
            .env(List.of(
                    new V1EnvVar().name("LAB_TYPE").value(template.getLabType()),
                    new V1EnvVar().name("LAB_ID").value(labId),
                    new V1EnvVar().name("TEMPLATE_ID").value(template.getId()),
                    // Add environment variables for JohnDoe template
                    new V1EnvVar().name("DEBIAN_FRONTEND").value("noninteractive")
            ))
            .resources(new V1ResourceRequirements()
                    .putLimitsItem("memory", new io.kubernetes.client.custom.Quantity("1Gi"))  // Increased for JohnDoe
                    .putLimitsItem("cpu", new io.kubernetes.client.custom.Quantity("500m"))
                    .putRequestsItem("memory", new io.kubernetes.client.custom.Quantity("512Mi"))
                    .putRequestsItem("cpu", new io.kubernetes.client.custom.Quantity("200m")));

    // Add workspace volume mount
    container.addVolumeMountsItem(new V1VolumeMount()
            .name("workspace")
            .mountPath("/workspace"));

    // Add volume mounts for docker labs
    if ("docker".equals(template.getLabType())) {
        container.addVolumeMountsItem(new V1VolumeMount()
                .name("docker-sock")
                .mountPath("/var/run/docker.sock"));
    }

    // Special configuration for JohnDoe template
    if ("johndoe".equals(template.getLabType())) {
        // Add privileged access for user management
        container.securityContext(new V1SecurityContext()
                .privileged(false)
                .runAsUser(0L)  // Run as root initially to create users
                .allowPrivilegeEscalation(true));
    }

    V1PodSpec spec = new V1PodSpec()
            .containers(List.of(container))
            .restartPolicy("Never")
            .activeDeadlineSeconds((long) (template.getDurationMinutes() * 60));

    // Add workspace volume
    spec.addVolumesItem(new V1Volume()
            .name("workspace")
            .emptyDir(new V1EmptyDirVolumeSource()));

    // Add volumes for docker labs
    if ("docker".equals(template.getLabType())) {
        spec.addVolumesItem(new V1Volume()
                .name("docker-sock")
                .hostPath(new V1HostPathVolumeSource().path("/var/run/docker.sock")));
    }

    Map<String, String> labels = new HashMap<>();
    labels.put("app", "lab");
    labels.put("template", template.getId());
    labels.put("labType", template.getLabType());
    labels.put("labId", labId);

    return new V1Pod()
            .metadata(new V1ObjectMeta()
                    .name(labId)
                    .labels(labels))
            .spec(spec);
}





    private Map<String, String> getLabImages() {
        Map<String, String> images = new HashMap<>();
        images.put("docker", "ubuntu:20.04");
        images.put("python", "python:3.9-slim");
        images.put("nodejs", "node:16-alpine");
        images.put("kubernetes", "bitnami/kubectl:latest");
        return images;
    }

    private List<String> getContainerCommand(String labType) {
        switch (labType) {
            case "docker":
                return List.of("/bin/bash", "-c", "apt-get update && apt-get install -y docker.io && sleep 7200");
            case "python":
                return List.of("/bin/sh", "-c", "pip install jupyter && sleep 7200");
            case "nodejs":
                return List.of("/bin/sh", "-c", "npm install -g nodemon && sleep 7200");
            case "kubernetes":
                return List.of("/bin/sh", "-c", "sleep 7200");
            default:
                return List.of("/bin/sh", "-c", "sleep 7200");
        }
    }

   
    private List<String> getTemplateContainerCommand(String labType) {
    // Keep container alive while setup process runs
    switch (labType) {
        case "docker":
            return List.of("/bin/bash", "-c", "sleep infinity");
        case "python":
            return List.of("/bin/sh", "-c", "sleep infinity");
        case "nodejs":
            return List.of("/bin/sh", "-c", "sleep infinity");
        case "johndoe":
            // For JohnDoe template, we want to ensure the container starts properly and keeps running
            return List.of("/bin/bash", "-c", 
                "apt-get update -y && " +
                "apt-get install -y sudo adduser && " +
                "service ssh start 2>/dev/null || true && " +
                "tail -f /dev/null");
        case "kubernetes":
            return List.of("/bin/sh", "-c", "sleep infinity");
        default:
            return List.of("/bin/sh", "-c", "sleep infinity");
    }
}
}