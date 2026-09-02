package com.neurosys.backend.repository;

import com.neurosys.backend.entity.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabRepository extends JpaRepository<Lab, String> {
    Optional<Lab> findByCodeIgnoreCase(String code);
    Optional<Lab> findByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
}
