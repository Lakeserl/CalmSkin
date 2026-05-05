package com.lakeserl.user_service.security;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, Object> redis;

    private String otpKey(String userId, String type)   { return "otp:" + userId + ":" + type; }
    private String refreshKey(String userId)            { return "refresh_token:" + userId; }
    private String loginAttemptKey(String email)        { return "login_attempts:" + email; }
    private String blacklistKey(String jti)             { return "blacklist_token:" + jti; }
    private String rateLimitKey(String ip, String ep)   { return "rate_limit:" + ip + ":" + ep; }

    public void saveOtp(String userId, String type, String hashedOtp, long ttlMinutes) {
        redis.opsForValue().set(otpKey(userId, type), hashedOtp, ttlMinutes, TimeUnit.MINUTES);
    }

    public String getOtp(String userId, String type) {
        Object val = redis.opsForValue().get(otpKey(userId, type));
        return val != null ? val.toString() : null;
    }

    public void deleteOtp(String userId, String type) {
        redis.delete(otpKey(userId, type));
    }

    public void saveRefreshToken(String userId, String tokenHash) {
        String key = refreshKey(userId);
        redis.opsForSet().add(key, tokenHash);
        redis.expire(key, 7, TimeUnit.DAYS);
    }

    public boolean isRefreshTokenValid(String userId, String tokenHash) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(refreshKey(userId), tokenHash));
    }

    public void revokeRefreshToken(String userId, String tokenHash) {
        redis.opsForSet().remove(refreshKey(userId), tokenHash);
    }

    public void revokeAllRefreshTokens(String userId) {
        redis.delete(refreshKey(userId));
    }

    public void blacklist(String jti, long ttlSeconds) {
        redis.opsForValue().set(blacklistKey(jti), "1", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(blacklistKey(jti)));
    }

    public int incrementLoginAttempt(String email) {
        String key = loginAttemptKey(email);
        Long count = redis.opsForValue().increment(key);
        if (count == 1) redis.expire(key, 15, TimeUnit.MINUTES);
        return count.intValue();
    }

    public boolean isLockedOut(String email) {
        Object val = redis.opsForValue().get(loginAttemptKey(email));
        return val != null && Integer.parseInt(val.toString()) >= 5;
    }

    public void resetLoginAttempt(String email) {
        redis.delete(loginAttemptKey(email));
    }

    public boolean checkRateLimit(String ip, String endpoint, int maxRequests, long windowSeconds) {
        String key = rateLimitKey(ip, endpoint);
        Long count = redis.opsForValue().increment(key);
        if (count == 1) redis.expire(key, windowSeconds, TimeUnit.SECONDS);
        return count <= maxRequests;
    }
    
}
