package com.example.be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "lab_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTemplate {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "lab_type", nullable = false)
    private String labType; 
    
    @Column(name = "base_image", nullable = false)
    private String baseImage; 
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
    
    @Column(name = "total_setup_time")
    private Integer totalSetupTime; 
    
    @Column(name = "success_criteria", columnDefinition = "TEXT")
    private String successCriteria; 
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SetupStep> setupSteps;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    public enum Difficulty {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}