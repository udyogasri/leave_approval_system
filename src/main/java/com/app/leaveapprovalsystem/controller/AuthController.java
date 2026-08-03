package com.app.leaveapprovalsystem.controller;

import com.app.leaveapprovalsystem.dto.*;
import com.app.leaveapprovalsystem.entity.User;
import com.app.leaveapprovalsystem.service.AuthService;
import com.app.leaveapprovalsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Register, login, and current-user info")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a user",
        description = """
            Role rules:
            - EMPLOYEE: public, no token needed.
            - ADMIN: public if no admin exists yet (first-time bootstrap). Requires ADMIN token once an admin exists.
            - MANAGER: always requires an ADMIN token.
            """
    )
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO dto) {

        UserResponseDTO created = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("User registered successfully", created));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO dto) {

        LoginResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(ApiResponseDTO.success("Login successful", response));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current authenticated user info")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getCurrentUser(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponseDTO.success("Current user",
                userService.getUserById(currentUser.getId())));
    }
}
