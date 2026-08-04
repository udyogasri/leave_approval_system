package com.app.leaveapprovalsystem.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyLeaveResponseDTO {

    private Long leaveId;
    private String processInstanceId;
    private String taskId;
    private String message;
}
