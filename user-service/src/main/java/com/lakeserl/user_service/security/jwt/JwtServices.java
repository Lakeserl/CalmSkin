package com.lakeserl.user_service.security.jwt;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.lakeserl.user_service.exception.TokenExpiredException;
import com.lakeserl.user_service.exception.TokenInvalidException;
import com.lakeserl.user_service.model.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtServices {

    @Value("${app.jwt.secret:${jwt.secret}}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiry;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiry;

    public String generateAccessToken(User user) {
        return buildToken(Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream()
                        .map(r -> "ROLE_" + r.getName().name())
                        .toList()),
                user.getEmail(), accessExpiry);
    }

    public String generateRefreshToken(User user) {
        return buildToken(
                Map.of("userId", user.getId().toString()),
                user.getEmail(),
                refreshExpiry);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiry) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti cho blacklist
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public String extractUserId(String token) {
        return extractClaim(token, c -> c.get("userId", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, c -> (List<String>) c.get("roles"));
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseAllClaims(token));
    }

    public boolean isValid(String token, UserDetails userDetails) {
        try {
            return extractUsername(token).equals(userDetails.getUsername());
        } catch (TokenExpiredException | TokenInvalidException e) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // SHA-256 hash refresh token trước khi lưu DB/Redis
    public String hash(String rawToken) {
        return DigestUtils.sha256Hex(rawToken);
    }

    private Claims parseAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            throw new TokenExpiredException();
        } catch (JwtException e) {
            log.warn("JWT invalid: {}", e.getMessage());
            throw new TokenInvalidException();
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
