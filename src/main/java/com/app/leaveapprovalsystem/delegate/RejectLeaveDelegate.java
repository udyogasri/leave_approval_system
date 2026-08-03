package com.app.leaveapprovalsystem.delegate;

import com.app.leaveapprovalsystem.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("rejectLeaveDelegate")
@RequiredArgsConstructor
@Slf4j
public class RejectLeaveDelegate implements JavaDelegate {

    private final LeaveService leaveService;

    @Override
    public void execute(DelegateExecution execution) {
        Long leaveId = ((Number) execution.getVariable("leaveId")).longValue();

        String approvedBy = (String) execution.getVariable("approvedBy");
        String comments   = (String) execution.getVariable("comments");

        leaveService.rejectLeaveById(leaveId);

        if (approvedBy != null || comments != null) {
            leaveService.setComments(leaveId, comments, approvedBy);
        }

        log.info("Leave rejected by delegate: leaveId={}, rejectedBy={}", leaveId, approvedBy);
    }
}
