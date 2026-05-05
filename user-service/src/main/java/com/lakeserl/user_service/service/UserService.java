package com.lakeserl.user_service.service;

import java.util.UUID;

import com.lakeserl.user_service.model.dto.UserDTO;
import com.lakeserl.user_service.model.dto.request.ChangePasswordRequest;
import com.lakeserl.user_service.model.dto.request.UpdateProfileRequest;
import com.lakeserl.user_service.model.entity.User;

public interface UserService {

    UserDTO getProfile(UUID userId);

    UserDTO updateProfile(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    void deactivateAccount(UUID userId);

    void deleteAccount(UUID userId);

    String uploadAvatar(UUID userId, org.springframework.web.multipart.MultipartFile file);

    void deleteAvatar(UUID userId);

    User findById(UUID userId);
}
