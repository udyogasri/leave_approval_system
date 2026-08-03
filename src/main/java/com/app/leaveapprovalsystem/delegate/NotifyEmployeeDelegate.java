package com.app.leaveapprovalsystem.delegate;

import com.app.leaveapprovalsystem.entity.LeaveRequest;
import com.app.leaveapprovalsystem.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("notifyEmployeeDelegate")
@RequiredArgsConstructor
@Slf4j
public class NotifyEmployeeDelegate implements JavaDelegate {

    private final LeaveService leaveService;

    @Override
    public void execute(DelegateExecution execution) {
        Long leaveId = ((Number) execution.getVariable("leaveId")).longValue();
        LeaveRequest leave = leaveService.getLeaveById(leaveId);

        String name  = leave.getUser().getFirstName() + " " + leave.getUser().getLastName();
        String email = leave.getUser().getEmail();

        log.info("Notification sent to '{}' ({}): Leave ID={} has been {}.",
                name, email, leaveId, leave.getStatus());

        execution.setVariable("notificationSent", true);
    }
}
