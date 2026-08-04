package com.app.leaveapprovalsystem.service;

import com.app.leaveapprovalsystem.dto.*;
import com.app.leaveapprovalsystem.entity.LeaveRequest;
import com.app.leaveapprovalsystem.entity.User;
import com.app.leaveapprovalsystem.exception.LeaveNotFoundException;
import com.app.leaveapprovalsystem.exception.UnauthorizedException;
import com.app.leaveapprovalsystem.mapper.LeaveMapper;
import com.app.leaveapprovalsystem.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final LeaveMapper leaveMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final UserService userService;

    // ── Employee: Apply Leave ─────────────────────────────────────────────────

    @Transactional
    public ApplyLeaveResponseDTO applyLeave(LeaveRequestDTO dto, User currentUser) {
        log.info("Leave application by userId={}", currentUser.getId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("userId",        currentUser.getId());
        variables.put("employeeName",  currentUser.getFirstName() + " " + currentUser.getLastName());
        variables.put("employeeEmail", currentUser.getEmail());
        variables.put("reason",        dto.getReason());
        variables.put("days",          dto.getDays());
        if (currentUser.getManager() != null) {
            variables.put("managerId", currentUser.getManager().getId());
        }

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "leaveApprovalProcess", variables);

        Long leaveId = ((Number) runtimeService.getVariable(instance.getId(), "leaveId")).longValue();

        LeaveRequest leave = getLeaveById(leaveId);
        leave.setProcessInstanceId(instance.getId());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leaveRepository.save(leave);

        Task task = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .taskDefinitionKey("managerApproval")
                .singleResult();

        log.info("Leave applied: leaveId={}, processInstanceId={}", leaveId, instance.getId());

        return ApplyLeaveResponseDTO.builder()
                .leaveId(leaveId)
                .processInstanceId(instance.getId())
                .taskId(task != null ? task.getId() : null)
                .message("Leave request submitted successfully.")
                .build();
    }

    // ── Employee: Own leaves ──────────────────────────────────────────────────

    public Page<LeaveResponseDTO> getMyLeaves(User currentUser, Pageable pageable) {
        return leaveRepository.findByUser(currentUser, pageable).map(leaveMapper::toResponseDTO);
    }

    // ── Employee: Notifications ───────────────────────────────────────────────

    public List<NotificationDTO> getMyNotifications(User currentUser) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskDefinitionKey("notifyEmployee")
                .orderByTaskCreateTime().asc()
                .list();

        return tasks.stream()
                .filter(t -> {
                    Object var = runtimeService.getVariable(t.getProcessInstanceId(), "leaveId");
                    if (var == null) return false;
                    return leaveRepository.findById(((Number) var).longValue())
                            .map(l -> l.getUser().getId().equals(currentUser.getId()))
                            .orElse(false);
                })
                .map(t -> {
                    Long leaveId = ((Number) runtimeService.getVariable(t.getProcessInstanceId(), "leaveId")).longValue();
                    LeaveRequest leave = getLeaveById(leaveId);
                    return NotificationDTO.builder()
                            .leaveId(leaveId)
                            .status(leave.getStatus())
                            .reason(leave.getReason())
                            .days(leave.getDays())
                            .comments(leave.getComments())
                            .message("Your leave request has been " + leave.getStatus())
                            .taskId(t.getId())
                            .build();
                })
                .toList();
    }

    // ── Employee: Acknowledge ─────────────────────────────────────────────────

    @Transactional
    public void acknowledgeNotification(String taskId, User currentUser) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new LeaveNotFoundException("Notification task not found: " + taskId);

        Object var = runtimeService.getVariable(task.getProcessInstanceId(), "leaveId");
        if (var != null) {
            LeaveRequest leave = leaveRepository.findById(((Number) var).longValue()).orElse(null);
            if (leave != null) {
                if (!leave.getUser().getId().equals(currentUser.getId())) {
                    throw new UnauthorizedException("This notification does not belong to you");
                }
                leave.setNotificationSent(true);
                leaveRepository.save(leave);
            }
        }

        taskService.complete(taskId);
        log.info("Notification acknowledged: taskId={}", taskId);
    }

    // ── Manager: Pending approvals ────────────────────────────────────────────

    public List<PendingTaskDTO> getPendingApprovals(User currentUser) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskDefinitionKey("managerApproval")
                .orderByTaskCreateTime().asc()
                .list();

        return tasks.stream()
                .filter(t -> {
                    Object var = runtimeService.getVariable(t.getProcessInstanceId(), "leaveId");
                    if (var == null) return false;
                    return leaveRepository.findById(((Number) var).longValue())
                            .map(l -> {
                                User mgr = l.getUser().getManager();
                                return mgr != null && mgr.getId().equals(currentUser.getId());
                            })
                            .orElse(false);
                })
                .map(t -> {
                    Long leaveId = ((Number) runtimeService.getVariable(t.getProcessInstanceId(), "leaveId")).longValue();
                    LeaveRequest leave = getLeaveById(leaveId);
                    String empName = leave.getUser().getFirstName() + " " + leave.getUser().getLastName();
                    return PendingTaskDTO.builder()
                            .taskId(t.getId())
                            .taskName(t.getName())
                            .leaveId(leaveId)
                            .employeeName(empName)
                            .department(leave.getUser().getDepartment())
                            .reason(leave.getReason())
                            .days(leave.getDays())
                            .leaveStatus(leave.getStatus())
                            .createdAt(t.getCreateTime())
                            .build();
                })
                .toList();
    }

    // ── Manager: Approve ──────────────────────────────────────────────────────

    @Transactional
    public void approveLeave(String taskId, LeaveDecisionRequestDTO dto, User currentUser) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new LeaveNotFoundException("Task not found: " + taskId);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved",   true);
        variables.put("approvedBy", currentUser.getEmail());
        variables.put("approvedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (dto != null && dto.getComments() != null) variables.put("comments", dto.getComments());

        taskService.complete(taskId, variables);
        log.info("Leave approved: taskId={}, by={}", taskId, currentUser.getEmail());
    }

    // ── Manager: Reject ───────────────────────────────────────────────────────

    @Transactional
    public void rejectLeave(String taskId, LeaveDecisionRequestDTO dto, User currentUser) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new LeaveNotFoundException("Task not found: " + taskId);

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved",   false);
        variables.put("approvedBy", currentUser.getEmail());
        variables.put("approvedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        if (dto != null && dto.getComments() != null) variables.put("comments", dto.getComments());

        taskService.complete(taskId, variables);
        log.info("Leave rejected: taskId={}, by={}", taskId, currentUser.getEmail());
    }

    // ── Manager: Team history ─────────────────────────────────────────────────

    public Page<LeaveResponseDTO> getTeamLeaves(User currentUser, Pageable pageable) {
        return leaveRepository.findByManagerId(currentUser.getId(), pageable).map(leaveMapper::toResponseDTO);
    }

    // ── Admin: All leaves ─────────────────────────────────────────────────────

    public Page<LeaveResponseDTO> getAllLeaves(Pageable pageable) {
        return leaveRepository.findAll(pageable).map(leaveMapper::toResponseDTO);
    }

    // ── Helpers (used by delegates) ───────────────────────────────────────────

    public LeaveRequest getLeaveById(Long id) {
        return leaveRepository.findById(id).orElseThrow(() -> new LeaveNotFoundException(id));
    }

    @Transactional
    public void approveLeaveById(Long id) {
        LeaveRequest leave = getLeaveById(id);
        leave.setStatus("APPROVED");
        leaveRepository.save(leave);
    }

    @Transactional
    public void rejectLeaveById(Long id) {
        LeaveRequest leave = getLeaveById(id);
        leave.setStatus("REJECTED");
        leaveRepository.save(leave);
    }

    @Transactional
    public void setComments(Long id, String comments, String approvedBy) {
        LeaveRequest leave = getLeaveById(id);
        leave.setComments(comments);
        leave.setApprovedBy(approvedBy);
        leaveRepository.save(leave);
    }
}
