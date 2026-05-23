package com.lakeserl.user_service.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.user_service.event.KafkaProducer;
import com.lakeserl.user_service.exception.*;
import com.lakeserl.user_service.model.dto.request.Login;
import com.lakeserl.user_service.model.dto.request.Register;
import com.lakeserl.user_service.model.dto.request.ResetPasswordRequest;
import com.lakeserl.user_service.model.dto.response.AuthResponse;
import com.lakeserl.user_service.model.entity.RefreshToken;
import com.lakeserl.user_service.model.entity.Role;
import com.lakeserl.user_service.model.entity.User;
import com.lakeserl.user_service.model.enums.RoleName;
import com.lakeserl.user_service.model.enums.Status;
import com.lakeserl.user_service.repository.RefreshTokenRepository;
import com.lakeserl.user_service.repository.RoleRepository;
import com.lakeserl.user_service.repository.UserRepository;
import com.lakeserl.user_service.security.RedisTokenService;
import com.lakeserl.user_service.security.jwt.JwtServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtServices jwtServices;
    private final RedisTokenService redisTokenService;
    private final KafkaProducer kafkaProducer;

    @Value("${jwt.access-expiration}")
    private long accessExpiry;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiry;

    @Value("${app.otp.length}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Transactional
    public void register(Register request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new PhoneAlreadyExistsException();
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .avatarUrl(request.getAvatarUrl())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .status(Status.UNVERIFIED)
                .roles(Set.of(customerRole))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        String otp = generateOtp();
        redisTokenService.saveOtp(user.getId().toString(), "EMAIL_VERIFY", DigestUtils.sha256Hex(otp), otpExpiryMinutes);

        kafkaProducer.sendUserRegistered(user.getId().toString(), user.getEmail());
        log.info("User registered: {}", maskEmail(user.getEmail()));
    }

    @Transactional
    public void verifyEmail(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String userId = user.getId().toString();
        if (redisTokenService.isOtpLockedOut(userId, "EMAIL_VERIFY")) {
            throw new OtpInvalidException("Too many failed attempts. Please request a new verification email.");
        }

        String stored = redisTokenService.getOtp(userId, "EMAIL_VERIFY");
        if (stored == null) throw new OtpExpiredException();
        if (!stored.equals(DigestUtils.sha256Hex(otp))) {
            redisTokenService.incrementOtpAttempt(userId, "EMAIL_VERIFY");
            throw new OtpInvalidException();
        }

        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        redisTokenService.deleteOtp(userId, "EMAIL_VERIFY");
        redisTokenService.resetOtpAttempt(userId, "EMAIL_VERIFY");

        kafkaProducer.sendUserEmailVerified(userId, user.getEmail());
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() != Status.UNVERIFIED) {
                return;
            }
            String otp = generateOtp();
            redisTokenService.saveOtp(user.getId().toString(), "EMAIL_VERIFY", DigestUtils.sha256Hex(otp), otpExpiryMinutes);
            kafkaProducer.sendUserRegistered(user.getId().toString(), user.getEmail());
        });
    }

    public AuthResponse login(Login request, String ipAddress, String deviceInfo) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException());

        if (redisTokenService.isLockedOut(request.getEmail())) {
            throw new InvalidCredentialsException("Account locked due to too many failed attempts");
        }
        if (user.getStatus() == Status.UNVERIFIED) throw new AccountNotVerifiedException();
        if (user.getStatus() == Status.BANNED) throw new AccountBannedException();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            redisTokenService.incrementLoginAttempt(request.getEmail());
            throw new InvalidCredentialsException();
        }

        redisTokenService.resetLoginAttempt(request.getEmail());

        String accessToken = jwtServices.generateAccessToken(user);
        String refreshToken = jwtServices.generateRefreshToken(user);
        String hashedRefresh = jwtServices.hash(refreshToken);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(hashedRefresh)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiry / 1000))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build());
        redisTokenService.saveRefreshToken(user.getId().toString(), hashedRefresh);

        kafkaProducer.sendUserLoggedIn(user.getId().toString(), ipAddress);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiry / 1000)
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        String email = jwtServices.extractUsername(rawRefreshToken);
        String userId = jwtServices.extractUserId(rawRefreshToken);
        String oldHash = jwtServices.hash(rawRefreshToken);

        if (!redisTokenService.isRefreshTokenValid(userId, oldHash)) {
            throw new TokenInvalidException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Token rotation: revoke old, issue new
        redisTokenService.revokeRefreshToken(userId, oldHash);
        refreshTokenRepository.findByTokenAndRevokedFalse(oldHash)
                .ifPresent(rt -> { rt.setRevoked(true); refreshTokenRepository.save(rt); });

        String newAccess = jwtServices.generateAccessToken(user);
        String newRefresh = jwtServices.generateRefreshToken(user);
        String newHash = jwtServices.hash(newRefresh);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(newHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiry / 1000))
                .build());
        redisTokenService.saveRefreshToken(userId, newHash);

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(accessExpiry / 1000)
                .build();
    }

    @Transactional
    public void logout(String accessToken) {
        String jti = jwtServices.extractJti(accessToken);
        long ttl = (jwtServices.extractExpiration(accessToken).getTime() - System.currentTimeMillis()) / 1000;
        if (ttl > 0) {
            redisTokenService.blacklist(jti, ttl);
        }

        String userId = jwtServices.extractUserId(accessToken);
        redisTokenService.revokeAllRefreshTokens(userId);
        refreshTokenRepository.findByUserIdAndRevokedFalse(UUID.fromString(userId))
                .forEach(rt -> { rt.setRevoked(true); refreshTokenRepository.save(rt); });
    }

    @Transactional
    public void logoutAll(String accessToken) {
        String jti = jwtServices.extractJti(accessToken);
        long ttl = (jwtServices.extractExpiration(accessToken).getTime() - System.currentTimeMillis()) / 1000;
        if (ttl > 0) redisTokenService.blacklist(jti, ttl);

        String userId = jwtServices.extractUserId(accessToken);
        redisTokenService.revokeAllRefreshTokens(userId);

        refreshTokenRepository.findByUserIdAndRevokedFalse(UUID.fromString(userId))
                .forEach(rt -> { rt.setRevoked(true); refreshTokenRepository.save(rt); });
    }

    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String otp = generateOtp();
            redisTokenService.saveOtp(user.getId().toString(), "RESET_PASSWORD", DigestUtils.sha256Hex(otp), otpExpiryMinutes);
            kafkaProducer.sendPasswordReset(user.getId().toString(), user.getEmail(), otp);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String userId = user.getId().toString();
        if (redisTokenService.isOtpLockedOut(userId, "RESET_PASSWORD")) {
            throw new OtpInvalidException("Too many failed attempts. Please request a new password reset.");
        }

        String stored = redisTokenService.getOtp(userId, "RESET_PASSWORD");
        if (stored == null) throw new OtpExpiredException();
        if (!stored.equals(DigestUtils.sha256Hex(request.getOtp()))) {
            redisTokenService.incrementOtpAttempt(userId, "RESET_PASSWORD");
            throw new OtpInvalidException();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTokenService.deleteOtp(userId, "RESET_PASSWORD");
        redisTokenService.resetOtpAttempt(userId, "RESET_PASSWORD");
        redisTokenService.revokeAllRefreshTokens(userId);
    }

    public void sendLoginOtp(String phoneNumber) {
        userRepository.findByPhoneNumber(phoneNumber).ifPresent(user -> {
            if (user.getStatus() == Status.BANNED) return;
            String otp = generateOtp();
            redisTokenService.saveOtp(user.getId().toString(), "LOGIN_OTP", DigestUtils.sha256Hex(otp), otpExpiryMinutes);
            kafkaProducer.sendPasswordReset(user.getId().toString(), user.getEmail(), otp);
        });
    }

    public AuthResponse loginWithOtp(String phoneNumber, String otp, String ipAddress, String deviceInfo) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new InvalidCredentialsException());
        if (user.getStatus() == Status.BANNED) throw new AccountBannedException();

        String userId = user.getId().toString();
        if (redisTokenService.isOtpLockedOut(userId, "LOGIN_OTP")) {
            throw new OtpInvalidException("Too many failed attempts. Please request a new OTP.");
        }

        String stored = redisTokenService.getOtp(userId, "LOGIN_OTP");
        if (stored == null) throw new OtpExpiredException();
        if (!stored.equals(DigestUtils.sha256Hex(otp))) {
            redisTokenService.incrementOtpAttempt(userId, "LOGIN_OTP");
            throw new OtpInvalidException();
        }

        redisTokenService.deleteOtp(userId, "LOGIN_OTP");
        redisTokenService.resetOtpAttempt(userId, "LOGIN_OTP");

        if (user.getStatus() == Status.UNVERIFIED) {
            user.setStatus(Status.ACTIVE);
            userRepository.save(user);
        }

        return issueTokens(user, ipAddress, deviceInfo);
    }

    public boolean validateToken(String token) {
        try {
            String jti = jwtServices.extractJti(token);
            if (redisTokenService.isBlacklisted(jti)) return false;
            jwtServices.extractUsername(token); // Will throw if invalid/expired
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private AuthResponse issueTokens(User user, String ipAddress, String deviceInfo) {
        String accessToken = jwtServices.generateAccessToken(user);
        String refreshToken = jwtServices.generateRefreshToken(user);
        String hashedRefresh = jwtServices.hash(refreshToken);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(hashedRefresh)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiry / 1000))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build());
        redisTokenService.saveRefreshToken(user.getId().toString(), hashedRefresh);

        kafkaProducer.sendUserLoggedIn(user.getId().toString(), ipAddress);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiry / 1000)
                .build();
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 3) return "***" + email.substring(at);
        return email.substring(0, 3) + "****" + email.substring(at);
    }
}
