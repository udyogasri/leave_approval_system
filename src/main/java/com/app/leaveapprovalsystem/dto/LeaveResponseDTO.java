package com.app.leaveapprovalsystem.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveResponseDTO {

    private Long id;
    private Long userId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private String reason;
    private Integer days;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String comments;
    private String approvedBy;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private String processInstanceId;
}
