package com.app.leaveapprovalsystem.repository;

import com.app.leaveapprovalsystem.entity.RoleName;
import com.app.leaveapprovalsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole_Name(RoleName name);

    Page<User> findByRole_Name(RoleName name, Pageable pageable);
}
