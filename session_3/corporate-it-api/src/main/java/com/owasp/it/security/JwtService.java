package com.owasp.it.security;

import com.owasp.it.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            String displayName = claims.get("name", String.class);
            List<String> roles = claims.get("roles", List.class);

            if (username == null || roles == null || roles.isEmpty()) {
                throw new InvalidTokenException("Token is missing required user claims");
            }

            return new AuthenticatedUser(username, displayName, roles);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired bearer token", ex);
        }
    }
}
