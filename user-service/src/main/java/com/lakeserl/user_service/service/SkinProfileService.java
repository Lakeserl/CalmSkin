package com.lakeserl.user_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.user_service.exception.UserNotFoundException;
import com.lakeserl.user_service.mapper.SkinProfileMapper;
import com.lakeserl.user_service.model.dto.request.SkinProfileRequest;
import com.lakeserl.user_service.model.dto.response.SkinProfileResponse;
import com.lakeserl.user_service.model.entity.SkinProfile;
import com.lakeserl.user_service.model.entity.User;
import com.lakeserl.user_service.repository.SkinProfileRepository;
import com.lakeserl.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SkinProfileService {

    private final SkinProfileRepository skinProfileRepository;
    private final UserRepository userRepository;
    private final SkinProfileMapper skinProfileMapper;

    public SkinProfileResponse getProfile(UUID userId) {
        SkinProfile profile = skinProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Skin profile not found"));
        return skinProfileMapper.toResponse(profile);
    }

    @Transactional
    public SkinProfileResponse createProfile(UUID userId, SkinProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        SkinProfile profile = skinProfileMapper.toEntity(request);
        profile.setUser(user);
        return skinProfileMapper.toResponse(skinProfileRepository.save(profile));
    }

    @Transactional
    public SkinProfileResponse updateProfile(UUID userId, SkinProfileRequest request) {
        SkinProfile profile = skinProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Skin profile not found"));

        skinProfileMapper.updateFromRequest(request, profile);
        return skinProfileMapper.toResponse(skinProfileRepository.save(profile));
    }
}
