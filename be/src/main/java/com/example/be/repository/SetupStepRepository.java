package com.example.be.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.be.model.SetupStep;

@Repository
public interface SetupStepRepository extends JpaRepository<SetupStep, String> {
    
    List<SetupStep> findByTemplateIdOrderByStepOrder(String templateId);
    
    Optional<SetupStep> findByTemplateIdAndStepOrder(String templateId, Integer stepOrder);
    
    @Query("SELECT COUNT(s) FROM SetupStep s WHERE s.template.id = :templateId")
    Long countStepsByTemplateId(@Param("templateId") String templateId);
}