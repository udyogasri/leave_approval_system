package com.app.leaveapprovalsystem.repository;

import com.app.leaveapprovalsystem.entity.Role;
import com.app.leaveapprovalsystem.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
