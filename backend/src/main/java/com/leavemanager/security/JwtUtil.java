package com.leavemanager.security;

import com.leavemanager.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * INTERVIEW NOTE:
 * This class handles all JWT (JSON Web Token) operations: generating new tokens upon login,
 * parsing token claims, and validating incoming tokens.
 *
 * It uses HMAC SHA-256 (HS256) symmetric signing key to sign and verify tokens, ensuring that
 * the client cannot modify their token's payload (e.g. changing their role or user ID) without invalidating it.
 */
@Component
@Slf4j
public class JwtUtil {

    private final Key signingKey;
    private final long jwtExpirationMs;

    // Injecting configurations from application.properties
    public JwtUtil(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {
        // Keys.hmacShaKeyFor creates a cryptographic Key from the raw secret bytes.
        // The secret must be at least 256 bits (32 bytes) long for HS256 algorithm.
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Extracts the username (Subject) from the token's claims.
     */
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    /**
     * Extracts the token expiration timestamp.
     */
    public Date extractExpiration(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration();
    }

    /**
     * Parses the JWT token and returns all claims.
     * Note: Jwts.parserBuilder() validates the signature automatically using the signingKey.
     * If the token has been tampered with or modified, it throws a SignatureException.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Compares the token's expiration date with the current system time.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generates a new JWT token for an authenticated user.
     * Adds custom claims like role, email, and name to the payload.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        return createToken(claims, user.getUsername());
    }

    /**
     * Builds the token: sets claims, subject, issued/expiration times,
     * signs it with our key using HS256, and serializes it to a compact string.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates if the token matches the user details loaded from the DB and is not expired.
     * Captures specific exceptions to aid in debugging/logging why validation failed.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            // 1. Verify the username in the token matches the DB user.
            // 2. Verify the token has not expired.
            // Note: parseClaimsJws already checked the signature integrity in extractAllClaims.
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
