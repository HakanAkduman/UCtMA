package com.app.uctma.backend.repository;

import com.app.uctma.backend.model.AuthPasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthPasswordPolicyRepository extends JpaRepository<AuthPasswordPolicy, Long> {
    List<AuthPasswordPolicy> findAllByOrderByIdAsc();
}
