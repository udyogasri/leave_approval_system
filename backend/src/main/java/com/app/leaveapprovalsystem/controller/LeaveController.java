package com.app.leaveapprovalsystem.controller;

import com.app.leaveapprovalsystem.dto.*;
import com.app.leaveapprovalsystem.entity.User;
import com.app.leaveapprovalsystem.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Tag(name = "Leave Management", description = "Apply, view, approve and reject leaves")
@SecurityRequirement(name = "bearerAuth")
public class LeaveController {

    private final LeaveService leaveService;

    // ── Employee: Apply ───────────────────────────────────────────────────────

    @PostMapping("/apply")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Apply for leave (Employee only)")
    public ResponseEntity<ApiResponseDTO<ApplyLeaveResponseDTO>> applyLeave(
            @Valid @RequestBody LeaveRequestDTO dto,
            @AuthenticationPrincipal User currentUser) {

        ApplyLeaveResponseDTO response = leaveService.applyLeave(dto, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success("Leave application submitted", response));
    }

    // ── Employee: View own leaves ─────────────────────────────────────────────

    @GetMapping("/my-leaves")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "View own leave history (Employee only)")
    public ResponseEntity<ApiResponseDTO<Page<LeaveResponseDTO>>> getMyLeaves(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10, sort = "appliedAt") Pageable pageable) {

        Page<LeaveResponseDTO> leaves = leaveService.getMyLeaves(currentUser, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success("Leave history retrieved", leaves));
    }

    // ── Employee: Notifications ───────────────────────────────────────────────

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "View pending notifications (Employee only)")
    public ResponseEntity<ApiResponseDTO<List<NotificationDTO>>> getNotifications(
            @AuthenticationPrincipal User currentUser) {

        List<NotificationDTO> notifications = leaveService.getMyNotifications(currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success("Notifications retrieved", notifications));
    }

    @PostMapping("/acknowledge/{taskId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Acknowledge notification (Employee only)")
    public ResponseEntity<ApiResponseDTO<Void>> acknowledgeNotification(
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {

        leaveService.acknowledgeNotification(taskId, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success("Notification acknowledged"));
    }

    // ── Manager: Pending approvals ────────────────────────────────────────────

    @GetMapping("/pending-approvals")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get pending leave approvals for manager")
    public ResponseEntity<ApiResponseDTO<List<PendingTaskDTO>>> getPendingApprovals(
            @AuthenticationPrincipal User currentUser) {

        List<PendingTaskDTO> tasks = leaveService.getPendingApprovals(currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success("Pending approvals retrieved", tasks));
    }

    @PostMapping("/approve/{taskId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Approve a leave request (Manager only)")
    public ResponseEntity<ApiResponseDTO<Void>> approveLeave(
            @PathVariable String taskId,
            @Valid @RequestBody(required = false) LeaveDecisionRequestDTO dto,
            @AuthenticationPrincipal User currentUser) {

        leaveService.approveLeave(taskId, dto, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success("Leave approved successfully"));
    }

    @PostMapping("/reject/{taskId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Reject a leave request (Manager only)")
    public ResponseEntity<ApiResponseDTO<Void>> rejectLeave(
            @PathVariable String taskId,
            @Valid @RequestBody(required = false) LeaveDecisionRequestDTO dto,
            @AuthenticationPrincipal User currentUser) {

        leaveService.rejectLeave(taskId, dto, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success("Leave rejected successfully"));
    }

    // ── Manager: Team history ─────────────────────────────────────────────────

    @GetMapping("/team-leaves")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "View team leave history (Manager only)")
    public ResponseEntity<ApiResponseDTO<Page<LeaveResponseDTO>>> getTeamLeaves(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10, sort = "appliedAt") Pageable pageable) {

        Page<LeaveResponseDTO> leaves = leaveService.getTeamLeaves(currentUser, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success("Team leaves retrieved", leaves));
    }
}
