package com.poc.orchestrator.repository;

import com.poc.orchestrator.model.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, String> {
    List<WorkflowInstance> findByDefinitionId(String definitionId);
}