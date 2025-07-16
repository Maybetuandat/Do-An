package com.example.be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "lab_instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lab {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "template_id")
    private String templateId;
    
    @Column(name = "lab_type")
    private String labType;
    
    @Enumerated(EnumType.STRING)
    private LabStatus status;
    
    @Column(name = "setup_status")
    @Enumerated(EnumType.STRING)
    private SetupStatus setupStatus;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "setup_started_at")
    private LocalDateTime setupStartedAt;
    
    @Column(name = "setup_completed_at")
    private LocalDateTime setupCompletedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "access_url")
    private String accessUrl;
    
    @Column(name = "pod_name")
    private String podName;
    
    @Column(name = "duration")
    private Integer duration;
    
    @OneToMany(mappedBy = "labInstance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SetupExecutionLog> setupLogs;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = LabStatus.CREATING;
        }
        if (setupStatus == null) {
            setupStatus = SetupStatus.INITIALIZING;
        }
    }
    
    public enum LabStatus {
        CREATING, READY, RUNNING, STOPPED, ERROR, EXPIRED
    }
    
    public enum SetupStatus {
        INITIALIZING, SETTING_UP, READY, FAILED
    }
}