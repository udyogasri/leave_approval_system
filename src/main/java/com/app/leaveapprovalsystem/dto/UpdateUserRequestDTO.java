package com.app.leaveapprovalsystem.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequestDTO {

    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    private String lastName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    // Profile fields — applicable for EMPLOYEE and MANAGER roles only
    @Size(min = 2, max = 100, message = "Department must be 2-100 characters")
    private String department;

    @Size(min = 2, max = 100, message = "Designation must be 2-100 characters")
    private String designation;

    private Long managerId;
}
