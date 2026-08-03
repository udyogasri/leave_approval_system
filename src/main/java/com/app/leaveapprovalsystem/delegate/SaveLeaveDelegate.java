package com.app.leaveapprovalsystem.delegate;

import com.app.leaveapprovalsystem.entity.LeaveRequest;
import com.app.leaveapprovalsystem.entity.User;
import com.app.leaveapprovalsystem.repository.LeaveRepository;
import com.app.leaveapprovalsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("saveLeaveDelegate")
@RequiredArgsConstructor
@Slf4j
public class SaveLeaveDelegate implements JavaDelegate {

    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Long userId = ((Number) execution.getVariable("userId")).longValue();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        LeaveRequest leave = new LeaveRequest();
        leave.setUser(user);
        leave.setReason((String) execution.getVariable("reason"));
        leave.setDays((Integer) execution.getVariable("days"));
        leave.setStatus("PENDING");

        leaveRepository.save(leave);
        execution.setVariable("leaveId", leave.getId());

        log.info("Leave saved: leaveId={}, userId={}", leave.getId(), userId);
    }
}
