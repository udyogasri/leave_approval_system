package com.app.leaveapprovalsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Integer days;

    @Column(nullable = false)
    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private String comments;

    private String approvedBy;

    private LocalDateTime appliedAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean notificationSent = false;

    @Column
    private String processInstanceId;

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
