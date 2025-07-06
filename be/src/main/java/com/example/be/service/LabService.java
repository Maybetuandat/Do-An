package com.example.be.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.be.dto.CreateLabRequest;
import com.example.be.dto.ExecuteCommandRequest;
import com.example.be.dto.CommandResultResponse;
import com.example.be.dto.LabResponse;
import com.example.be.model.Lab;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LabService {

    private final KubernetesService kubernetesService;
    private final Map<String, Lab> labStore = new ConcurrentHashMap<>();

    public LabService(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @PostConstruct
    public void init() {
        log.info("LabService initialized");
    }

    public LabResponse createLab(CreateLabRequest request) throws Exception {
        String labId = generateLabId(request.getUserId());
        
        // Create Kubernetes pod
        String podName = kubernetesService.createLabPod(labId, request);
        
        // Create lab entity
        Lab lab = Lab.builder()
                .id(labId)
                .userId(request.getUserId())
                .labType(request.getLabType())
                .status("Creating")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(request.getDuration()))
                .accessUrl(generateAccessUrl(labId))
                .podName(podName)
                .duration(request.getDuration())
                .build();

        labStore.put(labId, lab);
        log.info("Lab created: {}", labId);
        
        return convertToResponse(lab);
    }

    public List<LabResponse> getUserLabs(String userId) {
        return labStore.values().stream()
                .filter(lab -> lab.getUserId().equals(userId))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public void deleteLab(String labId) throws Exception {
        Lab lab = labStore.get(labId);
        if (lab == null) {
            throw new IllegalArgumentException("Lab not found: " + labId);
        }

        kubernetesService.deletePod(lab.getPodName());
        labStore.remove(labId);
        log.info("Lab deleted: {}", labId);
    }

    public String getLabStatus(String labId) throws Exception {
        Lab lab = labStore.get(labId);
        if (lab == null) {
            throw new IllegalArgumentException("Lab not found: " + labId);
        }

        String podStatus = kubernetesService.getPodStatus(lab.getPodName());
        
        // Update lab status
        lab.setStatus(podStatus);
        labStore.put(labId, lab);
        
        return podStatus;
    }

    public CommandResultResponse executeCommand(ExecuteCommandRequest request) throws Exception {
        Lab lab = labStore.get(request.getLabId());
        if (lab == null) {
            throw new IllegalArgumentException("Lab not found: " + request.getLabId());
        }

        log.info("Executing command '{}' in lab {}", request.getCommand(), request.getLabId());
        
        // Validate command safety (basic security check)
        if (!isCommandSafe(request.getCommand())) {
            return CommandResultResponse.builder()
                    .command(request.getCommand())
                    .output("")
                    .error("Command not allowed for security reasons")
                    .exitCode(-1)
                    .success(false)
                    .build();
        }

        return kubernetesService.executeCommand(lab.getPodName(), request.getCommand());
    }

    private boolean isCommandSafe(String command) {
        // Basic security check - block potentially dangerous commands
        String[] blockedCommands = {
            "rm -rf", "sudo", "su", "passwd", "shutdown", "reboot", 
            "kill", "killall", "pkill", "halt", "poweroff",
            "dd", "mkfs", "fdisk", "mount", "umount"
        };
        
        String lowercaseCommand = command.toLowerCase();
        for (String blocked : blockedCommands) {
            if (lowercaseCommand.contains(blocked)) {
                log.warn("Blocked potentially dangerous command: {}", command);
                return false;
            }
        }
        
        return true;
    }

    private String generateLabId(String userId) {
        return "lab-" + userId + "-" + System.currentTimeMillis();
    }

    private String generateAccessUrl(String labId) {
        return "http://192.168.122.93:30000/" + labId;
    }

    private LabResponse convertToResponse(Lab lab) {
        return LabResponse.builder()
                .id(lab.getId())
                .userId(lab.getUserId())
                .labType(lab.getLabType())
                .status(lab.getStatus())
                .createdAt(lab.getCreatedAt())
                .expiresAt(lab.getExpiresAt())
                .accessUrl(lab.getAccessUrl())
                .podName(lab.getPodName())
                .build();
    }
}