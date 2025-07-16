package com.example.be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "setup_execution_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupExecutionLog {
    
    @Id
    private String id;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(name = "step_title")
    private String stepTitle;
    
    @Column(name = "command", columnDefinition = "TEXT")
    private String command;
    
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    
    @Column(name = "output", columnDefinition = "TEXT")
    private String output;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "exit_code")
    private Integer exitCode;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "attempt_number")
    private Integer attemptNumber = 1;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_instance_id", nullable = false)
    private Lab labInstance;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setup_step_id", nullable = false)
    private SetupStep setupStep;
    
    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ExecutionStatus.RUNNING;
        }
    }
    
    public enum ExecutionStatus {
        PENDING, RUNNING, SUCCESS, FAILED, TIMEOUT, SKIPPED
    }
}