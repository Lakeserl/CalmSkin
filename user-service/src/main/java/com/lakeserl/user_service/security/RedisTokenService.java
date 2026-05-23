package com.lakeserl.user_service.security;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, Object> redis;

    private String otpKey(String userId, String type)        { return "otp:" + userId + ":" + type; }
    private String otpAttemptKey(String userId, String type) { return "otp_attempts:" + userId + ":" + type; }
    private String refreshHashKey(String tokenHash)          { return "refresh_token:hash:" + tokenHash; }
    private String refreshUserKey(String userId)             { return "refresh_token:user:" + userId; }
    private String loginAttemptKey(String email)             { return "login_attempts:" + email; }
    private String blacklistKey(String jti)                  { return "blacklist_token:" + jti; }
    private String rateLimitKey(String ip, String ep)        { return "rate_limit:" + ip + ":" + ep; }

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
        redis.opsForValue().set(refreshHashKey(tokenHash), userId, 7, TimeUnit.DAYS);
        redis.opsForSet().add(refreshUserKey(userId), tokenHash);
        redis.expire(refreshUserKey(userId), 7, TimeUnit.DAYS);
    }

    public boolean isRefreshTokenValid(String userId, String tokenHash) {
        Object storedUserId = redis.opsForValue().get(refreshHashKey(tokenHash));
        return storedUserId != null && storedUserId.toString().equals(userId);
    }

    public void revokeRefreshToken(String userId, String tokenHash) {
        redis.delete(refreshHashKey(tokenHash));
        redis.opsForSet().remove(refreshUserKey(userId), tokenHash);
    }

    public void revokeAllRefreshTokens(String userId) {
        Set<Object> hashes = redis.opsForSet().members(refreshUserKey(userId));
        if (hashes != null) {
            for (Object hash : hashes) {
                redis.delete(refreshHashKey(hash.toString()));
            }
        }
        redis.delete(refreshUserKey(userId));
    }

    public void blacklist(String jti, long ttlSeconds) {
        redis.opsForValue().set(blacklistKey(jti), "1", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(blacklistKey(jti)));
    }

    public int incrementLoginAttempt(String email) {
        String key = loginAttemptKey(email);
        redis.opsForValue().setIfAbsent(key, "0", 15, TimeUnit.MINUTES);
        Long count = redis.opsForValue().increment(key);
        if (count == null) return 1;
        return count.intValue();
    }

    public boolean isLockedOut(String email) {
        Object val = redis.opsForValue().get(loginAttemptKey(email));
        return val != null && Integer.parseInt(val.toString()) >= 5;
    }

    public void resetLoginAttempt(String email) {
        redis.delete(loginAttemptKey(email));
    }

    public int incrementOtpAttempt(String userId, String type) {
        String key = otpAttemptKey(userId, type);
        redis.opsForValue().setIfAbsent(key, "0", 15, TimeUnit.MINUTES);
        Long count = redis.opsForValue().increment(key);
        if (count == null) return 1;
        return count.intValue();
    }

    public boolean isOtpLockedOut(String userId, String type) {
        Object val = redis.opsForValue().get(otpAttemptKey(userId, type));
        return val != null && Integer.parseInt(val.toString()) >= 5;
    }

    public void resetOtpAttempt(String userId, String type) {
        redis.delete(otpAttemptKey(userId, type));
    }

    public boolean checkRateLimit(String ip, String endpoint, int maxRequests, long windowSeconds) {
        String key = rateLimitKey(ip, endpoint);
        redis.opsForValue().setIfAbsent(key, "0", windowSeconds, TimeUnit.SECONDS);
        Long count = redis.opsForValue().increment(key);
        if (count == null) return false;
        return count.intValue() <= maxRequests;
    }
    
}
