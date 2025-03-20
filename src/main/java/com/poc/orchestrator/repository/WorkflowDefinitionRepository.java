package com.poc.orchestrator.repository;


import com.poc.orchestrator.model.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, String> {
}