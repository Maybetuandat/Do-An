package com.example.be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "setup_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupStep {
    
    @Id
    private String id;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "setup_command", columnDefinition = "TEXT", nullable = false)
    private String setupCommand;
    
    @Column(name = "expected_exit_code")
    private Integer expectedExitCode = 0;
    
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 300;
    
    @Column(name = "retry_count")
    private Integer retryCount = 1;
    
    @Column(name = "continue_on_failure")
    private Boolean continueOnFailure = false;
    
    @Column(name = "working_directory")
    private String workingDirectory = "/";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private LabTemplate template;
}