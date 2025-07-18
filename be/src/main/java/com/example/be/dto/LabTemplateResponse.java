package com.example.be.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabTemplateResponse {
    private String id;
    private String name;
    private String description;
    private String labType;
    private String baseImage;
    private Integer durationMinutes;
    private String difficulty;
    private Integer totalSetupTime;
    private String successCriteria;
    private LocalDateTime createdAt;
    private String createdBy;
    private Boolean isActive;
}