package com.example.be.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.be.model.SetupExecutionLog;

@Repository
public interface SetupExecutionLogRepository extends JpaRepository<SetupExecutionLog, String> {
    
    List<SetupExecutionLog> findByLabInstanceIdOrderByStepOrder(String labInstanceId);
    
    List<SetupExecutionLog> findByLabInstanceIdAndStatus(String labInstanceId, SetupExecutionLog.ExecutionStatus status);
    
    Optional<SetupExecutionLog> findByLabInstanceIdAndStepOrder(String labInstanceId, Integer stepOrder);
    
    @Query("SELECT COUNT(l) FROM SetupExecutionLog l WHERE l.labInstance.id = :labInstanceId AND l.status = 'SUCCESS'")
    Long countSuccessfulStepsByLabInstanceId(@Param("labInstanceId") String labInstanceId);
}