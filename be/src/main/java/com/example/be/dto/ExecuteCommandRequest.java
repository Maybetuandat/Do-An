package com.example.be.dto;

import lombok.Data;

@Data
public class ExecuteCommandRequest {
    private String labId;
    private String command;
}