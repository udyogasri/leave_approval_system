package com.app.leaveapprovalsystem.service;

import com.app.leaveapprovalsystem.dto.*;
import com.app.leaveapprovalsystem.entity.RoleName;
import com.app.leaveapprovalsystem.entity.User;
import com.app.leaveapprovalsystem.exception.UserNotFoundException;
import com.app.leaveapprovalsystem.mapper.UserMapper;
import com.app.leaveapprovalsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // ── Read ──────────────────────────────────────────────────────────────────

    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    public Page<UserResponseDTO> getUsersByRole(RoleName roleName, Pageable pageable) {
        return userRepository.findByRole_Name(roleName, pageable).map(userMapper::toResponse);
    }

    public UserResponseDTO getUserById(Long id) {
        return userMapper.toResponse(getById(id));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO dto) {
        User user = getById(id);

        if (dto.getFirstName() != null)   user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null)    user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getDepartment() != null)  user.setDepartment(dto.getDepartment());
        if (dto.getDesignation() != null) user.setDesignation(dto.getDesignation());
        if (dto.getManagerId() != null) {
            User manager = getById(dto.getManagerId());
            user.setManager(manager);
        }

        log.info("User updated: userId={}", id);
        return userMapper.toResponse(userRepository.save(user));
    }

    // ── Delete / Enable / Disable ─────────────────────────────────────────────

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) throw new UserNotFoundException(id);
        userRepository.deleteById(id);
        log.info("User deleted: userId={}", id);
    }

    @Transactional
    public UserResponseDTO setEnabled(Long id, boolean enabled) {
        User user = getById(id);
        user.setEnabled(enabled);
        log.info("User {} enabled={}", id, enabled);
        return userMapper.toResponse(userRepository.save(user));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
    }
}
