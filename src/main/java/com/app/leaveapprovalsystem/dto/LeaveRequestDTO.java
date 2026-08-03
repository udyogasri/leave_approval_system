package com.app.leaveapprovalsystem.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestDTO {

    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be 10-500 characters")
    private String reason;

    @NotNull(message = "Number of days is required")
    @Min(value = 1, message = "Minimum 1 day required")
    @Max(value = 30, message = "Maximum 30 days allowed")
    private Integer days;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
