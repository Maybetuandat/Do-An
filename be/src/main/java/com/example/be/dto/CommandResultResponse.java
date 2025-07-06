package com.example.be.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommandResultResponse {
    private String command;
    private String output;
    private String error;
    private int exitCode;
    private boolean success;
}