package com.example.be.dto;

import lombok.Data;

@Data
public class CreateLabFromTemplateRequest {
    private String userId;
    private String templateId;
}