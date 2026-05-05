package com.lakeserl.user_service.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.user_service.event.KafkaProducer;
import com.lakeserl.user_service.exception.UserNotFoundException;
import com.lakeserl.user_service.mapper.UserMapper;
import com.lakeserl.user_service.model.dto.UserDTO;
import com.lakeserl.user_service.model.entity.AuditLog;
import com.lakeserl.user_service.model.entity.Role;
import com.lakeserl.user_service.model.entity.User;
import com.lakeserl.user_service.model.enums.RoleName;
import com.lakeserl.user_service.model.enums.Status;
import com.lakeserl.user_service.repository.AuditLogRepository;
import com.lakeserl.user_service.repository.RoleRepository;
import com.lakeserl.user_service.repository.UserRepository;
import com.lakeserl.user_service.security.RedisTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserMapper userMapper;
    private final KafkaProducer kafkaProducer;
    private final RedisTokenService redisTokenService;

    public Page<UserDTO> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    public UserDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    @Transactional
    public void banUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setStatus(Status.BANNED);
        userRepository.save(user);
        // Revoke all sessions
        redisTokenService.revokeAllRefreshTokens(id.toString());
        kafkaProducer.sendUserBanned(id.toString(), user.getEmail());
    }

    @Transactional
    public void unbanUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void updateRole(UUID id, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Role role = roleRepository.findByName(RoleName.valueOf(roleName.toUpperCase()))
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    public void forceResetPassword(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        // Gửi email reset password
        kafkaProducer.sendPasswordReset(id.toString(), user.getEmail(), "ADMIN_RESET");
    }

    public Page<AuditLog> getAuditLogs(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Map<String, Long> getStatsSummary() {
        long total = userRepository.count();
        List<User> all = userRepository.findAll();
        long active = all.stream().filter(u -> u.getStatus() == Status.ACTIVE).count();
        long banned = all.stream().filter(u -> u.getStatus() == Status.BANNED).count();
        long unverified = all.stream().filter(u -> u.getStatus() == Status.UNVERIFIED).count();
        return Map.of(
                "total", total,
                "active", active,
                "banned", banned,
                "unverified", unverified
        );
    }

    public byte[] exportUsersCsv() {
        List<User> users = userRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("id,email,fullName,phone,status,createdAt\n");
        for (User u : users) {
            sb.append(u.getId()).append(",")
              .append(u.getEmail()).append(",")
              .append(u.getFullName() != null ? u.getFullName() : "").append(",")
              .append(u.getPhoneNumber() != null ? u.getPhoneNumber() : "").append(",")
              .append(u.getStatus()).append(",")
              .append(u.getCreatedAt()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
