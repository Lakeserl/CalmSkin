package com.lakeserl.user_service.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.user_service.exception.InvalidPasswordException;
import com.lakeserl.user_service.exception.PhoneAlreadyExistsException;
import com.lakeserl.user_service.exception.UserNotFoundException;
import com.lakeserl.user_service.mapper.UserMapper;
import com.lakeserl.user_service.model.dto.UserDTO;
import com.lakeserl.user_service.model.dto.internal.UserInternalDTO;
import com.lakeserl.user_service.model.dto.request.ChangePasswordRequest;
import com.lakeserl.user_service.model.dto.request.UpdateProfileRequest;
import com.lakeserl.user_service.model.entity.User;
import com.lakeserl.user_service.model.enums.Status;
import com.lakeserl.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDTO getProfile(UUID userId) {
        User user = findById(userId);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDTO updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findById(userId);

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new PhoneAlreadyExistsException();
        }

        userMapper.updateFromRequest(request, user);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Old password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = findById(userId);
        user.setStatus(Status.INACTIVE);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = findById(userId);
        userRepository.delete(user);
    }

    @Override
    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    public List<UserInternalDTO> findAllByIds(List<UUID> ids) {
        return userRepository.findAllById(ids).stream()
                .map(this::toInternalDto)
                .toList();
    }

    @Override
    public List<UUID> findUserIdsByBirthday(int month, int day) {
        return userRepository.findUserIdsByBirthday(month, day);
    }

    @Override
    @Transactional
    public String uploadAvatar(UUID userId, org.springframework.web.multipart.MultipartFile file) {
        User user = findById(userId);

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new InvalidPasswordException("File size must be under 5MB");
        }

        // Validate by magic bytes, not the user-supplied Content-Type header
        String detectedMime = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
            throw new InvalidPasswordException("Only JPG, PNG, WEBP images allowed");
        }

        // Never use the original filename — always generate a safe random name
        String extension = detectedMime.substring(detectedMime.indexOf('/') + 1).replace("jpeg", "jpg");
        String safeFilename = UUID.randomUUID() + "." + extension;
        String avatarUrl = "/avatars/" + userId + "/" + safeFilename;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return avatarUrl;
    }

    @Override
    @Transactional
    public void deleteAvatar(UUID userId) {
        User user = findById(userId);
        user.setAvatarUrl(null);
        userRepository.save(user);
    }

    private String detectMimeType(org.springframework.web.multipart.MultipartFile file) {
        try (java.io.InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            if (h.length >= 3
                    && (h[0] & 0xFF) == 0xFF
                    && (h[1] & 0xFF) == 0xD8
                    && (h[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (h.length >= 8
                    && h[0] == (byte) 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                    && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
                return "image/png";
            }
            // WebP: "RIFF" at 0-3 and "WEBP" at 8-11
            if (h.length >= 12
                    && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
                return "image/webp";
            }
        } catch (java.io.IOException e) {
            log.warn("Failed to read upload header for MIME detection", e);
        }
        return "application/octet-stream";
    }

    private UserInternalDTO toInternalDto(User user) {
        return UserInternalDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .build();
    }

    @Override
    @Transactional
    public UserDTO updateAvatarUrl(UUID userId, String avatarUrl) {
        User user = findById(userId);
        user.setAvatarUrl(avatarUrl);
        return userMapper.toDto(userRepository.save(user));
    }
}
