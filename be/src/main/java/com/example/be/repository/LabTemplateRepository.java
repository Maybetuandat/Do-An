package com.example.be.repository;
import com.example.be.model.LabTemplate;
import com.example.be.model.SetupStep;
import com.example.be.model.Lab;
import com.example.be.model.SetupExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabTemplateRepository extends JpaRepository<LabTemplate, String> {
    
    List<LabTemplate> findByLabTypeAndIsActiveTrue(String labType);
    
    List<LabTemplate> findByIsActiveTrueOrderByCreatedAtDesc();
    
    List<LabTemplate> findByDifficultyAndIsActiveTrue(LabTemplate.Difficulty difficulty);
    
    @Query("SELECT t FROM LabTemplate t WHERE t.isActive = true AND " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<LabTemplate> searchByKeyword(@Param("keyword") String keyword);
}




