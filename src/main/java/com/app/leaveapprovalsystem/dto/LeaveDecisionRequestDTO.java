package com.app.leaveapprovalsystem.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveDecisionRequestDTO {

    @Size(max = 500, message = "Comments must be at most 500 characters")
    private String comments;
}
