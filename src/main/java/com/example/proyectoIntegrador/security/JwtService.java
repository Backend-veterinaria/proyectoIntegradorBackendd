package com.example.proyectoIntegrador.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Genera y valida los tokens JWT (firmados con HMAC-SHA256) que identifican al usuario
 * en cada petición después del login.
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        // Lanza WeakKeyException al arrancar si la clave tiene menos de 256 bits
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(int userId, String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * @return el usuario del token, o vacío si el token está mal formado, tiene una firma
     * inválida o ya expiró
     */
    public Optional<AuthenticatedUser> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new AuthenticatedUser(
                    Integer.parseInt(claims.getSubject()),
                    claims.get(CLAIM_USERNAME, String.class),
                    Role.fromName(claims.get(CLAIM_ROLE, String.class))));

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT no valido: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }
}
