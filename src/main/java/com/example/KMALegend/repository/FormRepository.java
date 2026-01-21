package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormRepository extends JpaRepository<Form, Long>, JpaSpecificationExecutor<Form> {
    List<Form> findByIsActiveTrue();
    List<Form> findByCreatorId(Long creatorId);
}
