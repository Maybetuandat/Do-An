package com.example.be.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.be.dto.CreateLabFromTemplateRequest;
import com.example.be.dto.LabResponse;
import com.example.be.dto.LabTemplateResponse;
import com.example.be.dto.SetupStepResponse;
import com.example.be.dto.SetupExecutionLogResponse;
import com.example.be.model.Lab;
import com.example.be.model.SetupExecutionLog;
import com.example.be.service.LabTemplateService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class LabTemplateController {

    private final LabTemplateService labTemplateService;

    @GetMapping
    public ResponseEntity<List<LabTemplateResponse>> getAllTemplates() {
        log.info("Fetching all active lab templates");
        List<LabTemplateResponse> templates = labTemplateService.getAllActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/type/{labType}")
    public ResponseEntity<List<LabTemplateResponse>> getTemplatesByType(@PathVariable String labType) {
        log.info("Fetching templates for lab type: {}", labType);
        List<LabTemplateResponse> templates = labTemplateService.getTemplatesByType(labType);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<LabTemplateResponse> getTemplate(@PathVariable String templateId) {
        try {
            log.info("Fetching template: {}", templateId);
            LabTemplateResponse template = labTemplateService.getTemplateById(templateId);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            log.error("Template not found: {}", templateId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{templateId}/steps")
    public ResponseEntity<List<SetupStepResponse>> getTemplateSteps(@PathVariable String templateId) {
        try {
            log.info("Fetching setup steps for template: {}", templateId);
            List<SetupStepResponse> steps = labTemplateService.getTemplateSteps(templateId);
            return ResponseEntity.ok(steps);
        } catch (IllegalArgumentException e) {
            log.error("Template not found: {}", templateId);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/create-lab")
    public ResponseEntity<LabResponse> createLabFromTemplate(@RequestBody CreateLabFromTemplateRequest request) {
        try {
            log.info("Creating lab from template {} for user {}", request.getTemplateId(), request.getUserId());
            Lab lab = labTemplateService.createLabFromTemplate(request);
            LabResponse response = convertToLabResponse(lab);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid template request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to create lab from template: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/labs/{labId}/setup-logs")
    public ResponseEntity<List<SetupExecutionLogResponse>> getLabSetupLogs(@PathVariable String labId) {
        try {
            log.info("Fetching setup logs for lab: {}", labId);
            List<SetupExecutionLog> logs = labTemplateService.getLabSetupLogs(labId);
            List<SetupExecutionLogResponse> response = logs.stream()
                    .map(this::convertToLogResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch setup logs for lab {}: {}", labId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private LabResponse convertToLabResponse(Lab lab) {
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

    private SetupExecutionLogResponse convertToLogResponse(SetupExecutionLog log) {
        return SetupExecutionLogResponse.builder()
                .id(log.getId())
                .stepOrder(log.getStepOrder())
                .stepTitle(log.getStepTitle())
                .command(log.getCommand())
                .status(log.getStatus().toString())
                .output(log.getOutput())
                .errorMessage(log.getErrorMessage())
                .exitCode(log.getExitCode())
                .executionTimeMs(log.getExecutionTimeMs())
                .attemptNumber(log.getAttemptNumber())
                .startedAt(log.getStartedAt())
                .completedAt(log.getCompletedAt())
                .build();
    }
}