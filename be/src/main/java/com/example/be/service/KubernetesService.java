package com.example.be.service;


import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.be.dto.CreateLabRequest;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KubernetesService {

    private CoreV1Api api;
    private static final String NAMESPACE = "default";

    @PostConstruct
    public void init() throws Exception {
        ApiClient client = Config.defaultClient();
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
}
