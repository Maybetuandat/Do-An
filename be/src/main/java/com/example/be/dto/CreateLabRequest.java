package com.example.be.dto;

import lombok.Data;

@Data
public class CreateLabRequest {
    
    
    private String userId;
    
    
    private String labType;   //maybe we have docker, python, git ,etc, ..
    
    
    
    private Integer duration = 7200; // 2 hours default
}