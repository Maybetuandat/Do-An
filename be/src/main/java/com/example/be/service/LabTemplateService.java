package com.example.be.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.be.dto.CreateLabFromTemplateRequest;
import com.example.be.dto.LabTemplateResponse;
import com.example.be.dto.SetupStepResponse;
import com.example.be.model.Lab;
import com.example.be.model.LabTemplate;
import com.example.be.model.SetupStep;
import com.example.be.model.SetupExecutionLog;
import com.example.be.repository.LabTemplateRepository;
import com.example.be.repository.SetupStepRepository;
import com.example.be.repository.LabRepository;
import com.example.be.repository.SetupExecutionLogRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabTemplateService {

    private final LabTemplateRepository labTemplateRepository;
    private final SetupStepRepository setupStepRepository;
    private final LabRepository labRepository;
    private final SetupExecutionLogRepository setupExecutionLogRepository;
    private final KubernetesService kubernetesService;

    @PostConstruct
    public void initializeDefaultTemplates() {
        if (labTemplateRepository.count() == 0) {
            createDefaultTemplates();
        }
    }

    private void createDefaultTemplates() {
        log.info("Creating default lab templates...");
        
        // Python Development Template
        createPythonTemplate();
        
        // Docker Development Template
        createDockerTemplate();
        
        // Node.js Development Template
        createNodejsTemplate();
        
        // JohnDoe User Template - NEW
        createJohnDoeTemplate();
        
        log.info("Default templates created successfully");
    }

    private void createJohnDoeTemplate() {
        String templateId = "johndoe-user-template";
        
        LabTemplate johnDoeTemplate = LabTemplate.builder()
                .id(templateId)
                .name("JohnDoe User Environment")
                .description("Complete development environment with johndoe user setup and development tools")
                .labType("johndoe")
                .baseImage("ubuntu:20.04")
                .durationMinutes(150)
                .difficulty(LabTemplate.Difficulty.INTERMEDIATE)
                .totalSetupTime(480) // 8 minutes
                .successCriteria("johndoe user created and environment ready with development tools")
                .createdBy("system")
                .isActive(true)
                .build();

        labTemplateRepository.save(johnDoeTemplate);

        // Setup steps for JohnDoe template - FIXED
        createSetupStep(templateId, 1, "Wait for System Ready", 
                "Wait for container to be fully ready and release any locks",
                "sleep 15 && echo 'System ready'", 0, 30);

        createSetupStep(templateId, 2, "Check and Wait for APT Lock", 
                "Ensure no apt processes are running",
                "while fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 || fuser /var/lib/apt/lists/lock >/dev/null 2>&1; do echo 'Waiting for apt lock...'; sleep 5; done && echo 'APT ready'", 0, 120);

        createSetupStep(templateId, 3, "Update System and Install Basic Tools", 
                "Update package manager and install essential system tools",
                "DEBIAN_FRONTEND=noninteractive apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y curl wget git vim sudo passwd adduser", 0, 180);

        createSetupStep(templateId, 4, "Create JohnDoe User", 
                "Create johndoe user with home directory and bash shell",
                "adduser --disabled-password --gecos '' johndoe && echo 'johndoe:password123' | chpasswd", 0, 30);

        createSetupStep(templateId, 5, "Grant Sudo Privileges", 
                "Add johndoe to sudo group for administrative privileges",
                "usermod -aG sudo johndoe", 0, 10);

        createSetupStep(templateId, 6, "Setup JohnDoe Home Directory", 
                "Create workspace and setup basic directories for johndoe",
                "su - johndoe -c 'mkdir -p /home/johndoe/workspace /home/johndoe/projects /home/johndoe/scripts'", 0, 20);

        createSetupStep(templateId, 7, "Install Development Tools", 
                "Install Python, Node.js, and other development tools",
                "DEBIAN_FRONTEND=noninteractive apt-get install -y python3 python3-pip nodejs npm build-essential", 0, 240);

        createSetupStep(templateId, 8, "Setup Git Configuration", 
                "Configure git for johndoe user",
                "su - johndoe -c 'git config --global user.name \"John Doe\" && git config --global user.email \"johndoe@example.com\"'", 0, 15);

        createSetupStep(templateId, 9, "Install Python Packages for JohnDoe", 
                "Install essential Python packages in johndoe's environment",
                "su - johndoe -c 'pip3 install --user jupyter pandas numpy matplotlib requests flask'", 0, 180);

        createSetupStep(templateId, 10, "Setup Bash Profile", 
                "Configure bash profile and aliases for johndoe",
                "su - johndoe -c 'echo \"export PATH=\\$HOME/.local/bin:\\$PATH\" >> /home/johndoe/.bashrc && echo \"alias ll=\\\"ls -la\\\"\" >> /home/johndoe/.bashrc && echo \"alias workspace=\\\"cd /home/johndoe/workspace\\\"\" >> /home/johndoe/.bashrc'", 0, 15);

        createSetupStep(templateId, 11, "Create Sample Projects", 
                "Create sample projects and scripts for johndoe",
                "su - johndoe -c 'cd /home/johndoe/workspace && echo \"print(\\\"Hello from JohnDoe Lab!\\\")\" > hello.py && echo \"console.log(\\\"Hello from JohnDoe Node.js!\\\");\" > hello.js && echo \"#!/bin/bash\\necho \\\"Welcome JohnDoe!\\\"\\nwhoami\\npwd\" > /home/johndoe/scripts/welcome.sh && chmod +x /home/johndoe/scripts/welcome.sh'", 0, 30);

        createSetupStep(templateId, 12, "Setup SSH Keys", 
                "Generate SSH keys for johndoe user",
                "su - johndoe -c 'mkdir -p /home/johndoe/.ssh && ssh-keygen -t rsa -b 4096 -f /home/johndoe/.ssh/id_rsa -N \"\" && cat /home/johndoe/.ssh/id_rsa.pub > /home/johndoe/.ssh/authorized_keys && chmod 600 /home/johndoe/.ssh/authorized_keys && chmod 700 /home/johndoe/.ssh'", 0, 30);

        createSetupStep(templateId, 13, "Verify JohnDoe Environment", 
                "Verify that johndoe user environment is properly configured",
                "su - johndoe -c 'whoami && pwd && ls -la /home/johndoe/ && python3 --version && node --version && git --version'", 0, 30);

        createSetupStep(templateId, 14, "Configure Default User Login", 
                "Setup environment to switch to johndoe user by default",
                "echo '#!/bin/bash' > /etc/profile.d/johndoe-login.sh && echo 'if [ \"$USER\" = \"root\" ] && [ -t 0 ]; then' >> /etc/profile.d/johndoe-login.sh && echo '  exec su - johndoe' >> /etc/profile.d/johndoe-login.sh && echo 'fi' >> /etc/profile.d/johndoe-login.sh && chmod +x /etc/profile.d/johndoe-login.sh", 0, 15);

        createSetupStep(templateId, 15, "Final Verification", 
                "Final check that everything is working",
                "su - johndoe -c 'echo \"JohnDoe environment setup complete!\" && /home/johndoe/scripts/welcome.sh'", 0, 20);
    }

    // Existing template creation methods remain the same...
    private void createPythonTemplate() {
        String templateId = "python-dev-template";
        
        LabTemplate pythonTemplate = LabTemplate.builder()
                .id(templateId)
                .name("Python Development Environment")
                .description("Complete Python development environment with popular packages and tools")
                .labType("python")
                .baseImage("python:3.9-slim")
                .durationMinutes(120)
                .difficulty(LabTemplate.Difficulty.BEGINNER)
                .totalSetupTime(300) // 5 minutes
                .successCriteria("Python environment ready with pip, jupyter, and common packages installed")
                .createdBy("system")
                .isActive(true)
                .build();

        labTemplateRepository.save(pythonTemplate);

        // Setup steps for Python template
        createSetupStep(templateId, 1, "Update System Packages", 
                "Update package manager and install basic tools",
                "apt-get update && apt-get install -y curl wget git vim", 0, 120);

        createSetupStep(templateId, 2, "Install Python Packages", 
                "Install essential Python packages",
                "pip install --upgrade pip && pip install jupyter pandas numpy matplotlib requests flask", 0, 180);

        createSetupStep(templateId, 3, "Setup Jupyter", 
                "Configure Jupyter notebook",
                "jupyter notebook --generate-config && echo \"c.NotebookApp.ip = '0.0.0.0'\" >> ~/.jupyter/jupyter_notebook_config.py", 0, 60);

        createSetupStep(templateId, 4, "Create Sample Project", 
                "Create a sample Python project structure",
                "mkdir -p /workspace/sample-project && cd /workspace/sample-project && echo 'print(\"Hello from Python Lab!\")' > hello.py", 0, 30);
    }

    private void createDockerTemplate() {
        String templateId = "docker-dev-template";
        
        LabTemplate dockerTemplate = LabTemplate.builder()
                .id(templateId)
                .name("Docker Development Environment")
                .description("Docker development environment with Docker-in-Docker capability")
                .labType("docker")
                .baseImage("docker:20.10-dind")
                .durationMinutes(180)
                .difficulty(LabTemplate.Difficulty.INTERMEDIATE)
                .totalSetupTime(240) // 4 minutes
                .successCriteria("Docker daemon running and able to build/run containers")
                .createdBy("system")
                .isActive(true)
                .build();

        labTemplateRepository.save(dockerTemplate);

        createSetupStep(templateId, 1, "Start Docker Daemon", 
                "Start Docker daemon in background",
                "dockerd-entrypoint.sh &", 0, 30);

        createSetupStep(templateId, 2, "Wait for Docker", 
                "Wait for Docker daemon to be ready",
                "sleep 10 && docker info", 0, 30);

        createSetupStep(templateId, 3, "Install Docker Compose", 
                "Install Docker Compose tool",
                "apk add --no-cache docker-compose", 0, 60);

        createSetupStep(templateId, 4, "Create Sample Dockerfile", 
                "Create a sample Dockerfile for testing",
                "mkdir -p /workspace/docker-demo && cd /workspace/docker-demo && echo 'FROM alpine:latest\nRUN echo \"Hello from Docker Lab!\"\nCMD [\"echo\", \"Container is running!\"]' > Dockerfile", 0, 30);

        createSetupStep(templateId, 5, "Build Sample Image", 
                "Build the sample Docker image",
                "cd /workspace/docker-demo && docker build -t sample-app .", 0, 60);
    }

    private void createNodejsTemplate() {
        String templateId = "nodejs-dev-template";
        
        LabTemplate nodejsTemplate = LabTemplate.builder()
                .id(templateId)
                .name("Node.js Development Environment")
                .description("Node.js development environment with popular frameworks and tools")
                .labType("nodejs")
                .baseImage("node:16-alpine")
                .durationMinutes(90)
                .difficulty(LabTemplate.Difficulty.BEGINNER)
                .totalSetupTime(180) // 3 minutes
                .successCriteria("Node.js environment ready with npm packages and sample app")
                .createdBy("system")
                .isActive(true)
                .build();

        labTemplateRepository.save(nodejsTemplate);

        createSetupStep(templateId, 1, "Install System Tools", 
                "Install basic development tools",
                "apk add --no-cache git vim curl", 0, 60);

        createSetupStep(templateId, 2, "Create Sample Project", 
                "Create a sample Node.js project",
                "mkdir -p /workspace/nodejs-app && cd /workspace/nodejs-app && npm init -y", 0, 30);

        createSetupStep(templateId, 3, "Install Dependencies", 
                "Install popular Node.js packages",
                "cd /workspace/nodejs-app && npm install express nodemon cors dotenv", 0, 90);

        createSetupStep(templateId, 4, "Create Sample App", 
                "Create a sample Express.js application",
                "cd /workspace/nodejs-app && echo 'const express = require(\"express\");\nconst app = express();\napp.get(\"/\", (req, res) => res.json({message: \"Hello from Node.js Lab!\"}));\napp.listen(3000, () => console.log(\"Server running on port 3000\"));' > app.js", 0, 30);
    }

    private void createSetupStep(String templateId, int stepOrder, String title, String description, 
                                String command, int expectedExitCode, int timeoutSeconds) {
        SetupStep step = SetupStep.builder()
                .id(UUID.randomUUID().toString())
                .stepOrder(stepOrder)
                .title(title)
                .description(description)
                .setupCommand(command)
                .expectedExitCode(expectedExitCode)
                .timeoutSeconds(timeoutSeconds)
                .retryCount(2)
                .continueOnFailure(false)
                .workingDirectory("/")
                .build();
        
        // Set template reference
        LabTemplate template = labTemplateRepository.findById(templateId).orElse(null);
        step.setTemplate(template);
        
        setupStepRepository.save(step);
    }

    // Rest of the methods remain the same...
    public List<LabTemplateResponse> getAllActiveTemplates() {
        return labTemplateRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LabTemplateResponse> getTemplatesByType(String labType) {
        return labTemplateRepository.findByLabTypeAndIsActiveTrue(labType)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public LabTemplateResponse getTemplateById(String templateId) {
        LabTemplate template = labTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        return convertToResponse(template);
    }

    public List<SetupStepResponse> getTemplateSteps(String templateId) {
        return setupStepRepository.findByTemplateIdOrderByStepOrder(templateId)
                .stream()
                .map(this::convertStepToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Lab createLabFromTemplate(CreateLabFromTemplateRequest request) throws Exception {
        LabTemplate template = labTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + request.getTemplateId()));

        String labId = "lab-" + request.getUserId() + "-" + System.currentTimeMillis();
        
        // Create lab instance
        Lab lab = Lab.builder()
                .id(labId)
                .userId(request.getUserId())
                .templateId(template.getId())
                .labType(template.getLabType())
                .status(Lab.LabStatus.CREATING)
                .setupStatus(Lab.SetupStatus.INITIALIZING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(template.getDurationMinutes()))
                .accessUrl(generateAccessUrl(labId))
                .duration(template.getDurationMinutes() * 60) // Convert to seconds
                .build();

        labRepository.save(lab);

        // Create Kubernetes pod with template configuration
        String podName = kubernetesService.createLabPodFromTemplate(labId, template);
        lab.setPodName(podName);
        lab.setSetupStatus(Lab.SetupStatus.SETTING_UP);
        lab.setSetupStartedAt(LocalDateTime.now());
        labRepository.save(lab);

        // Start asynchronous setup process
        executeTemplateSetup(lab, template);

        log.info("Lab created from template: {} for user: {}", template.getName(), request.getUserId());
        return lab;
    }

    private void executeTemplateSetup(Lab lab, LabTemplate template) {
        // Execute setup steps asynchronously
        new Thread(() -> {
            try {
                waitForPodRunning(lab.getPodName(), 300); 
                List<SetupStep> steps = setupStepRepository.findByTemplateIdOrderByStepOrder(template.getId());
                boolean allSuccess = true;

                for (SetupStep step : steps) {
                    boolean stepSuccess = executeSetupStep(lab, step);
                    if (!stepSuccess && !step.getContinueOnFailure()) {
                        allSuccess = false;
                        break;
                    }
                }

                // Update lab status
                if (allSuccess) {
                    lab.setSetupStatus(Lab.SetupStatus.READY);
                    lab.setStatus(Lab.LabStatus.RUNNING);
                    lab.setSetupCompletedAt(LocalDateTime.now());
                } else {
                    lab.setSetupStatus(Lab.SetupStatus.FAILED);
                    lab.setStatus(Lab.LabStatus.ERROR);
                }
                
                labRepository.save(lab);
                log.info("Template setup completed for lab: {} with status: {}", lab.getId(), lab.getSetupStatus());

            } catch (Exception e) {
                log.error("Template setup failed for lab: {}", lab.getId(), e);
                lab.setSetupStatus(Lab.SetupStatus.FAILED);
                lab.setStatus(Lab.LabStatus.ERROR);
                labRepository.save(lab);
            }
        }).start();
    }

    private void waitForPodRunning(String podName, int timeoutSeconds) throws Exception {
        for (int i = 0; i < timeoutSeconds; i += 10) {
            try {
                String status = kubernetesService.getPodStatus(podName);
                if ("Running".equals(status)) {
                    log.info("Pod {} is now running", podName);
                    return;
                }
                Thread.sleep(10000); // Wait 10 seconds
            } catch (Exception e) {
                log.warn("Error checking pod status: {}", e.getMessage());
            }
        }
        throw new RuntimeException("Pod did not reach running state within timeout");
    }

    private boolean executeSetupStep(Lab lab, SetupStep step) {
        String logId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();

        SetupExecutionLog executionLog = SetupExecutionLog.builder()
                .id(logId)
                .stepOrder(step.getStepOrder())
                .stepTitle(step.getTitle())
                .command(step.getSetupCommand())
                .status(SetupExecutionLog.ExecutionStatus.RUNNING)
                .attemptNumber(1)
                .startedAt(startTime)
                .labInstance(lab)
                .setupStep(step)
                .build();

        setupExecutionLogRepository.save(executionLog);

        for (int attempt = 1; attempt <= step.getRetryCount(); attempt++) {
            try {
                log.info("Executing step {} (attempt {}) for lab {}: {}", 
                        step.getStepOrder(), attempt, lab.getId(), step.getTitle());

                // Execute command in pod
                var result = kubernetesService.executeSetupCommand(lab.getPodName(), step.getSetupCommand(), 
                        step.getTimeoutSeconds(), step.getWorkingDirectory());

                LocalDateTime endTime = LocalDateTime.now();
                long executionTime = java.time.Duration.between(startTime, endTime).toMillis();

                executionLog.setCompletedAt(endTime);
                executionLog.setExecutionTimeMs(executionTime);
                executionLog.setOutput(result.getOutput());
                executionLog.setErrorMessage(result.getError());
                executionLog.setExitCode(result.getExitCode());
                executionLog.setAttemptNumber(attempt);

                if (result.getExitCode() == step.getExpectedExitCode()) {
                    executionLog.setStatus(SetupExecutionLog.ExecutionStatus.SUCCESS);
                    setupExecutionLogRepository.save(executionLog);
                    log.info("Step {} completed successfully for lab {}", step.getStepOrder(), lab.getId());
                    return true;
                } else {
                    if (attempt == step.getRetryCount()) {
                        executionLog.setStatus(SetupExecutionLog.ExecutionStatus.FAILED);
                        setupExecutionLogRepository.save(executionLog);
                        log.error("Step {} failed after {} attempts for lab {}", 
                                step.getStepOrder(), attempt, lab.getId());
                        return false;
                    } else {
                        log.warn("Step {} failed (attempt {}), retrying for lab {}", 
                                step.getStepOrder(), attempt, lab.getId());
                        Thread.sleep(2000); // Wait before retry
                    }
                }

            } catch (Exception e) {
                log.error("Error executing step {} (attempt {}) for lab {}: {}", 
                        step.getStepOrder(), attempt, lab.getId(), e.getMessage());
                
                if (attempt == step.getRetryCount()) {
                    executionLog.setStatus(SetupExecutionLog.ExecutionStatus.FAILED);
                    executionLog.setErrorMessage("Execution error: " + e.getMessage());
                    executionLog.setCompletedAt(LocalDateTime.now());
                    setupExecutionLogRepository.save(executionLog);
                    return false;
                }
            }
        }

        return false;
    }

    public List<SetupExecutionLog> getLabSetupLogs(String labId) {
        return setupExecutionLogRepository.findByLabInstanceIdOrderByStepOrder(labId);
    }

    private String generateAccessUrl(String labId) {
        return "http://192.168.122.93:30000/" + labId;
    }

    private LabTemplateResponse convertToResponse(LabTemplate template) {
        return LabTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .labType(template.getLabType())
                .baseImage(template.getBaseImage())
                .durationMinutes(template.getDurationMinutes())
                .difficulty(template.getDifficulty().toString())
                .totalSetupTime(template.getTotalSetupTime())
                .successCriteria(template.getSuccessCriteria())
                .createdAt(template.getCreatedAt())
                .createdBy(template.getCreatedBy())
                .isActive(template.getIsActive())
                .build();
    }

    private SetupStepResponse convertStepToResponse(SetupStep step) {
        return SetupStepResponse.builder()
                .id(step.getId())
                .stepOrder(step.getStepOrder())
                .title(step.getTitle())
                .description(step.getDescription())
                .setupCommand(step.getSetupCommand())
                .expectedExitCode(step.getExpectedExitCode())
                .timeoutSeconds(step.getTimeoutSeconds())
                .retryCount(step.getRetryCount())
                .continueOnFailure(step.getContinueOnFailure())
                .workingDirectory(step.getWorkingDirectory())
                .build();
    }
}