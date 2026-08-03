package com.app.leaveapprovalsystem.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    private Long leaveId;
    private String status;
    private String reason;
    private Integer days;
    private String comments;
    private String message;
    private String taskId;
}
