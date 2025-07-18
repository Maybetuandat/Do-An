package com.example.be.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.be.service.LabTemplateService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final LabTemplateService labTemplateService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing application data...");
        // Template initialization is handled in @PostConstruct of LabTemplateService
        log.info("Application data initialization completed");
    }
}