package com.example.be.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetupStepResponse {
    private String id;
    private Integer stepOrder;
    private String title;
    private String description;
    private String setupCommand;
    private Integer expectedExitCode;
    private Integer timeoutSeconds;
    private Integer retryCount;
    private Boolean continueOnFailure;
    private String workingDirectory;
}