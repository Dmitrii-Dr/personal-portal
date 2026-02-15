package com.dmdr.personal.portal.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final String secretKey;
    private final String issuer;
    private final String audience;
    private static final long EXPIRATION_TIME_MS = 10 * 60 * 1000L; // 10 minutes

    public JwtService(
            @Value("${JWT_SECRET_KEY}") String secretKey,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience) {
        this.secretKey = secretKey;
        this.issuer = issuer;
        this.audience = audience;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, Set<String> roles, UUID sessionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME_MS);

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .audience()
                .add(audience)
                .and()
                .claim("roles", roles)
                .claim("sid", sessionId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    public UUID extractSessionId(String token) {
        Claims claims = extractClaims(token);
        String sessionId = claims.get("sid", String.class);
        return sessionId == null ? null : UUID.fromString(sessionId);
    }

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        Claims claims = extractClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Set) {
            return (Set<String>) rolesObj;
        } else if (rolesObj instanceof List) {
            return ((List<?>) rolesObj).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    public boolean isValidToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the issued at (iat) date from the JWT token.
     *
     * @param token the JWT token
     * @return the date when the token was issued, or null if not present
     */
    public Date getIssuedAt(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getIssuedAt();
        } catch (Exception e) {
            return null;
        }
    }
}
