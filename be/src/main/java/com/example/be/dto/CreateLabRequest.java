package com.example.be.dto;

import lombok.Data;

@Data
public class CreateLabRequest {
    
    
    private String userId;
    
    
    private String labType;
    
    
    
    private Integer duration = 7200; // 2 hours default
}