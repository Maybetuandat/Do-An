package com.example.be.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.be.dto.CreateLabRequest;
import com.example.be.dto.ExecuteCommandRequest;
import com.example.be.dto.CommandResultResponse;
import com.example.be.dto.LabResponse;
import com.example.be.service.LabService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/labs")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    @PostMapping("/create")
    public ResponseEntity<LabResponse> createLab(@RequestBody CreateLabRequest request) {
        try {
            log.info("Creating lab for user: {} with type: {}", request.getUserId(), request.getLabType());
            LabResponse lab = labService.createLab(request);
            return ResponseEntity.ok(lab);
        } catch (Exception e) {
            log.error("Failed to create lab: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LabResponse>> getUserLabs(@PathVariable String userId) {
        log.info("Fetching labs for user: {}", userId);
        List<LabResponse> labs = labService.getUserLabs(userId);
        return ResponseEntity.ok(labs);
    }

    @DeleteMapping("/{labId}")
    public ResponseEntity<Void> deleteLab(@PathVariable String labId) {
        try {
            log.info("Deleting lab: {}", labId);
            labService.deleteLab(labId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete lab {}: {}", labId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getLabTypes() {
        return ResponseEntity.ok(List.of("docker", "python", "nodejs", "kubernetes"));
    }

    @GetMapping("/{labId}/status")
    public ResponseEntity<String> getLabStatus(@PathVariable String labId) {
        try {
            String status = labService.getLabStatus(labId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get lab status for {}: {}", labId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<CommandResultResponse> executeCommand(@RequestBody ExecuteCommandRequest request) {
        try {
            log.info("Executing command '{}' in lab {}", request.getCommand(), request.getLabId());
            CommandResultResponse result = labService.executeCommand(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to execute command in lab {}: {}", request.getLabId(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                CommandResultResponse.builder()
                    .command(request.getCommand())
                    .output("")
                    .error("Failed to execute command: " + e.getMessage())
                    .exitCode(-1)
                    .success(false)
                    .build()
            );
        }
    }

    @GetMapping("/{labId}/suggested-commands")
    public ResponseEntity<List<String>> getSuggestedCommands(@PathVariable String labId) {
        // Get suggested commands based on lab type
        try {
            // You might want to get lab type from lab service
            List<String> commands = List.of(
                "ls -la",
                "pwd",
                "whoami",
                "ps aux",
                "df -h",
                "free -h",
                "uname -a",
                "cat /etc/os-release"
            );
            return ResponseEntity.ok(commands);
        } catch (Exception e) {
            log.error("Failed to get suggested commands for lab {}: {}", labId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}