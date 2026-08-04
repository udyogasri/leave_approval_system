package com.app.leaveapprovalsystem.dto;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingTaskDTO {

    private String taskId;
    private String taskName;
    private Long leaveId;
    private String employeeName;
    private String department;
    private String reason;
    private Integer days;
    private String leaveStatus;
    private Date createdAt;
}
