package com.example.be.dto;


import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SetupExecutionLogResponse {
    private String id;
    private Integer stepOrder;
    private String stepTitle;
    private String command;
    private String status;
    private String output;
    private String errorMessage;
    private Integer exitCode;
    private Long executionTimeMs;
    private Integer attemptNumber;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}