package com.workmates.common.security;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Signs and verifies the single access token (HS256). There is no refresh token.
 *
 * <p>Claims: {@code sub} = user ID, {@code role}, {@code iat}, {@code exp}. Because the role never
 * changes after registration, a request needs no database lookup to find the user.
 */
@Component
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration ttl;

    /**
     * @param secret the Base64 signing key from {@code JWT_SECRET}; at least 32 random bytes, otherwise
     *               the application fails at startup. Changing it logs everyone out.
     * @param ttl    how long a token stays valid ({@code JWT_TTL}, for example {@code 7d})
     */
    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.ttl}") Duration ttl) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.ttl = ttl;
    }

    /** Creates a signed token for the user. It expires {@code ttl} after now. */
    public String issue(Long userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /**
     * Checks the signature and expiry and reads the user from the token.
     *
     * @throws BusinessException UNAUTHORIZED if the token is malformed, tampered with or expired
     */
    public CurrentUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new CurrentUser(Long.valueOf(claims.getSubject()),
                    Role.valueOf(claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            // IllegalArgumentException covers a blank token, a non-numeric subject and an unknown role
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
