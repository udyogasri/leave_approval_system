package com.app.leaveapprovalsystem.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String role;
    private boolean enabled;

    // Populated for EMPLOYEE and MANAGER roles only
    private String employeeCode;
    private String department;
    private String designation;
    private LocalDate joiningDate;
    private Long managerId;
    private String managerName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
