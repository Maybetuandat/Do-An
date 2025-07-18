package com.example.be.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.be.dto.CreateLabRequest;
import com.example.be.dto.ExecuteCommandRequest;
import com.example.be.dto.CommandResultResponse;
import com.example.be.dto.LabResponse;
import com.example.be.model.Lab;
import com.example.be.repository.LabRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LabService {

    private final KubernetesService kubernetesService;
    private final LabRepository labRepository;

    public LabService(KubernetesService kubernetesService, LabRepository labRepository) {
        this.kubernetesService = kubernetesService;
        this.labRepository = labRepository;
    }

    @PostConstruct
    public void init() {
        log.info("LabService initialized");
        // Clean up any orphaned labs on startup
        cleanupExpiredLabs();
    }

    @Transactional
    public LabResponse createLab(CreateLabRequest request) throws Exception {
        String labId = generateLabId(request.getUserId());
        
        // Create Kubernetes pod
        String podName = kubernetesService.createLabPod(labId, request);
        
        // Create lab entity
        Lab lab = Lab.builder()
                .id(labId)
                .userId(request.getUserId())
                .labType(request.getLabType())
                .status(Lab.LabStatus.CREATING)
                .setupStatus(Lab.SetupStatus.READY) // Simple labs don't need setup
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(request.getDuration()))
                .accessUrl(generateAccessUrl(labId))
                .podName(podName)
                .duration(request.getDuration())
                .build();

        lab = labRepository.save(lab);
        log.info("Lab created: {}", labId);
        
        return convertToResponse(lab);
    }

    public List<LabResponse> getUserLabs(String userId) {
        return labRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LabResponse> getAllLabs() {
        return labRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteLab(String labId) throws Exception {
        Optional<Lab> labOpt = labRepository.findById(labId);
        if (labOpt.isEmpty()) {
            throw new IllegalArgumentException("Lab not found: " + labId);
        }

        Lab lab = labOpt.get();
        try {
            kubernetesService.deletePod(lab.getPodName());
        } catch (Exception e) {
            log.warn("Failed to delete pod for lab {}: {}", labId, e.getMessage());
        }
        
        labRepository.delete(lab);
        log.info("Lab deleted: {}", labId);
    }

    @Transactional
    public String getLabStatus(String labId) throws Exception {
        Optional<Lab> labOpt = labRepository.findById(labId);
        if (labOpt.isEmpty()) {
            throw new IllegalArgumentException("Lab not found: " + labId);
        }

        Lab lab = labOpt.get();
        
        // Check if lab is expired
        if (lab.getExpiresAt().isBefore(LocalDateTime.now())) {
            lab.setStatus(Lab.LabStatus.EXPIRED);
            labRepository.save(lab);
            return "EXPIRED";
        }

        try {
            String podStatus = kubernetesService.getPodStatus(lab.getPodName());
            
            // Update lab status based on pod status
            Lab.LabStatus newStatus;
            switch (podStatus) {
                case "Creating":
                    newStatus = Lab.LabStatus.CREATING;
                    break;
                case "Running":
                    newStatus = Lab.LabStatus.RUNNING;
                    break;
                case "Stopped":
                    newStatus = Lab.LabStatus.STOPPED;
                    break;
                default:
                    newStatus = Lab.LabStatus.ERROR;
                    break;
            }
            
            lab.setStatus(newStatus);
            labRepository.save(lab);
            
            return newStatus.toString();
        } catch (Exception e) {
            log.error("Failed to get pod status for lab {}: {}", labId, e.getMessage());
            lab.setStatus(Lab.LabStatus.ERROR);
            labRepository.save(lab);
            return "ERROR";
        }
    }

    public CommandResultResponse executeCommand(ExecuteCommandRequest request) throws Exception {
        Optional<Lab> labOpt = labRepository.findById(request.getLabId());
        if (labOpt.isEmpty()) {
            throw new IllegalArgumentException("Lab not found: " + request.getLabId());
        }

        Lab lab = labOpt.get();
        
        // Check if lab is expired
        if (lab.getExpiresAt().isBefore(LocalDateTime.now())) {
            return CommandResultResponse.builder()
                    .command(request.getCommand())
                    .output("")
                    .error("Lab has expired")
                    .exitCode(-1)
                    .success(false)
                    .build();
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

    @Transactional
    public void cleanupExpiredLabs() {
        log.info("Cleaning up expired labs...");
        List<Lab> expiredLabs = labRepository.findExpiredLabs();
        
        for (Lab lab : expiredLabs) {
            try {
                log.info("Cleaning up expired lab: {}", lab.getId());
                kubernetesService.deletePod(lab.getPodName());
                lab.setStatus(Lab.LabStatus.EXPIRED);
                labRepository.save(lab);
            } catch (Exception e) {
                log.warn("Failed to cleanup expired lab {}: {}", lab.getId(), e.getMessage());
            }
        }
        
        log.info("Cleaned up {} expired labs", expiredLabs.size());
    }

    public Optional<Lab> getLabById(String labId) {
        return labRepository.findById(labId);
    }

    public Optional<Lab> getLabByPodName(String podName) {
        return labRepository.findByPodName(podName);
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
                .status(lab.getStatus().toString())
                .createdAt(lab.getCreatedAt())
                .expiresAt(lab.getExpiresAt())
                .accessUrl(lab.getAccessUrl())
                .podName(lab.getPodName())
                .build();
    }
}