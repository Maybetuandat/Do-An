package com.example.be.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.be.model.Lab;

@Repository
public interface LabRepository extends JpaRepository<Lab, String> {
    
    List<Lab> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<Lab> findByTemplateIdAndStatus(String templateId, Lab.LabStatus status);
    
    List<Lab> findByStatusAndSetupStatus(Lab.LabStatus status, Lab.SetupStatus setupStatus);
    
    Optional<Lab> findByPodName(String podName);
    
    @Query("SELECT l FROM Lab l WHERE l.status = 'RUNNING' AND l.expiresAt < CURRENT_TIMESTAMP")
    List<Lab> findExpiredLabs();
}
