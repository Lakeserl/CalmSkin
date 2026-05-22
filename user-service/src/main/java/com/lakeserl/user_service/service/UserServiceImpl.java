package com.lakeserl.user_service.service;

import java.util.List;
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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

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

        String contentType = file.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|png|webp)")) {
            throw new InvalidPasswordException("Only JPG, PNG, WEBP images allowed");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new InvalidPasswordException("File size must be under 5MB");
        }

        String avatarUrl = "/avatars/" + userId + "/" + file.getOriginalFilename();
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

    private UserInternalDTO toInternalDto(User user) {
        return UserInternalDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .build();
    }
}
