package com.app.leaveapprovalsystem.repository;

import com.app.leaveapprovalsystem.entity.LeaveRequest;
import com.app.leaveapprovalsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByUser(User user, Pageable pageable);

    @Query("SELECT l FROM LeaveRequest l WHERE l.user.manager.id = :managerId")
    Page<LeaveRequest> findByManagerId(@Param("managerId") Long managerId, Pageable pageable);
}
