package com.app.leaveapprovalsystem.service;

import com.app.leaveapprovalsystem.dto.*;
import com.app.leaveapprovalsystem.entity.Role;
import com.app.leaveapprovalsystem.entity.User;
import com.app.leaveapprovalsystem.exception.InvalidCredentialsException;
import com.app.leaveapprovalsystem.exception.UnauthorizedException;
import com.app.leaveapprovalsystem.mapper.UserMapper;
import com.app.leaveapprovalsystem.repository.UserRepository;
import com.app.leaveapprovalsystem.security.JwtUtil;
import com.app.leaveapprovalsystem.util.EmployeeCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public UserResponseDTO register(RegisterRequestDTO dto) {

        if (dto.getRole() == null || dto.getRole().isBlank()) {
            throw new IllegalArgumentException("Role is required. Allowed: EMPLOYEE, MANAGER, ADMIN");
        }

        Role role = parseRole(dto.getRole());

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered");
        }

        // ADMIN registration:
        //   - if no admin exists yet → allowed for anyone (bootstrap)
        //   - if an admin exists    → requires an authenticated ADMIN token
        if (role == Role.ADMIN) {
            if (userRepository.existsByRole(Role.ADMIN)) {
                requireAdminToken(role);
            }
            // else: first admin — allow through without a token
        }

        // MANAGER registration always requires an authenticated ADMIN token
        if (role == Role.MANAGER) {
            requireAdminToken(role);
        }

        User.UserBuilder builder = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(dto.getPhoneNumber())
                .role(role)
                .enabled(true);

        if (role == Role.EMPLOYEE || role == Role.MANAGER) {
            if (dto.getDepartment() == null || dto.getDepartment().isBlank()) {
                throw new IllegalArgumentException("Department is required for role " + role);
            }
            if (dto.getDesignation() == null || dto.getDesignation().isBlank()) {
                throw new IllegalArgumentException("Designation is required for role " + role);
            }
            builder.employeeCode(EmployeeCodeGenerator.generate(dto.getDepartment()))
                   .department(dto.getDepartment())
                   .designation(dto.getDesignation())
                   .joiningDate(LocalDate.now());
        }

        User user = userRepository.save(builder.build());
        log.info("User registered: userId={}, role={}", user.getId(), role);
        return userMapper.toResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public LoginResponseDTO login(LoginRequestDTO dto) {
        log.info("Login attempt: {}", dto.getEmail());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));
        } catch (AuthenticationException ex) {
            log.warn("Login failed: {}", dto.getEmail());
            throw new InvalidCredentialsException();
        }

        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);

        log.info("Login successful: userId={}, role={}", user.getId(), user.getRole());

        return LoginResponseDTO.builder()
                .accessToken(token)
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireAdminToken(Role role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                       .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new UnauthorizedException(
                    "An ADMIN token is required to register a " + role + " account");
        }
    }

    private Role parseRole(String roleStr) {
        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid role '" + roleStr + "'. Allowed: EMPLOYEE, MANAGER, ADMIN");
        }
    }
}
