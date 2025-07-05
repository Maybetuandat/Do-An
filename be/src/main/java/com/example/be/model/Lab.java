package com.example.be.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lab {
    private String id;
    private String userId;
    private String labType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String accessUrl;
    private String podName;
    private Integer duration;
}
