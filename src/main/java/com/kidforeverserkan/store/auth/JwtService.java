package com.kidforeverserkan.store.auth;

import com.kidforeverserkan.store.config.JwtConfig;
import com.kidforeverserkan.store.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
@AllArgsConstructor
@Service
public class JwtService {
    private final JwtConfig jwtConfig;

    public Jwt generateAccessToken(User user) {
        return generateToken(user, jwtConfig.getAccessTokenExpiration());
    }

    public Jwt generateRefreshToken(User user) {
        return generateToken(user, jwtConfig.getRefreshTokenExpiration());
    }

    private Jwt generateToken(User user, long tokenExpiration) {

        var claims = Jwts.claims()
                .subject(user.getId().toString())
                .add("email", user.getEmail())
                .add("name", user.getName())
                .add("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * tokenExpiration))
                .build();

        return new Jwt(claims, jwtConfig.getSecretKey());
    }

    // Treats *any* unparseable, malformed, unsigned, or expired token the
    // same way: as "not authenticated," not as a server error. Without this,
    // an expired token was handled fine, but a garbage/tampered token (or an
    // empty string) threw a raw JwtException/IllegalArgumentException that
    // propagated uncaught out of JwtAuthenticationFilter on every protected
    // endpoint, and out of AuthController#refresh for the refresh cookie.
    public Jwt parseToken(String token) {
        try {
            var claims = getClaims(token);
            return  new Jwt(claims, jwtConfig.getSecretKey());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }



    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtConfig.getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getPayload();

    }
}
