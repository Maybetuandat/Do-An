package com.example.be.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabResponse {
    private String id;
    private String userId;
    private String labType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String accessUrl;
    private String podName;
}