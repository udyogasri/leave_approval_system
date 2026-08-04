package com.app.leaveapprovalsystem.controller;

import com.app.leaveapprovalsystem.dto.*;
import com.app.leaveapprovalsystem.entity.RoleName;
import com.app.leaveapprovalsystem.entity.User;
import com.app.leaveapprovalsystem.service.LeaveService;
import com.app.leaveapprovalsystem.service.UserService;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management — admin operations and own-profile endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final LeaveService leaveService;

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users, paginated (Admin only)")
    public ResponseEntity<ApiResponseDTO<Page<UserResponseDTO>>> getAllUsers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.success("Users retrieved", userService.getAllUsers(pageable)));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Filter users by role: ADMIN, MANAGER, EMPLOYEE (Admin only)")
    public ResponseEntity<ApiResponseDTO<Page<UserResponseDTO>>> getUsersByRole(
            @PathVariable String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.success("Users retrieved",
                userService.getUsersByRole(RoleName.valueOf(role.toUpperCase()), pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any user by ID (Admin only)")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success("User retrieved", userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update any user — name, phone, department, designation, managerId (Admin only)")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success("User updated", userService.updateUser(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a user (Admin only)")
    public ResponseEntity<ApiResponseDTO<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponseDTO.success("User deleted"));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable a user account (Admin only)")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success("User enabled", userService.setEnabled(id, true)));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable a user account (Admin only)")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success("User disabled", userService.setEnabled(id, false)));
    }

    @GetMapping("/reports/leaves")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "All leave requests — full report (Admin only)")
    public ResponseEntity<ApiResponseDTO<Page<LeaveResponseDTO>>> getAllLeaveReports(
            @PageableDefault(size = 20, sort = "appliedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.success("Leave report retrieved", leaveService.getAllLeaves(pageable)));
    }

    // ── Own profile (any authenticated user) ─────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get own profile")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponseDTO.success("Profile retrieved",
                userService.getUserById(currentUser.getId())));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update own profile — name, phone, department, designation")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success("Profile updated",
                userService.updateUser(currentUser.getId(), dto)));
    }
}
