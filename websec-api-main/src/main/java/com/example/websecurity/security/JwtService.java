package com.example.websecurity.security;

import com.example.websecurity.persistence.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Fix vuln/jwt-verification:
 *
 * VULNERABLE pattern (class repo branch) - only decodes base64, never
 * verifies the cryptographic signature:
 *
 *   String payloadJson = new String(Base64.getDecoder().decode(parts[1]));
 *   Map<String, Object> map = mapper.readValue(payloadJson, Map.class);
 *   return Jwts.claims(map);   // <-- no signature check at all
 *
 * With that code any attacker can:
 *   1. Take a valid token, decode the payload
 *   2. Change the "sub" (email) to any other user
 *   3. Re-encode with a random/no signature
 *   4. The server accepts it as legitimate
 *
 * FIX: use parseClaimsJws() with the server-side signing key.
 * jjwt throws a SignatureException when the signature does not match,
 * so a tampered or arbitrarily-signed token is rejected before any
 * claims are ever read.
 *
 * Additionally, tokens now carry an expiration (exp claim) so a stolen
 * token cannot be replayed indefinitely.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${websec.jwt-secret}")
    private String secretKey;

    // Token validity: 8 hours
    private static final long EXPIRATION_MS = 8 * 60 * 60 * 1000L;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String generateAccessToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("tokenType", "ACCESS");

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * A token is valid only when:
     * 1. The cryptographic signature matches (enforced inside extractAllClaims).
     * 2. The subject matches the loaded UserDetails.
     * 3. The token has not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    public String extractExtraClaim(String claimName, String token) {
        return extractAllClaims(token).get(claimName, String.class);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * FIX: parseClaimsJws() cryptographically verifies the HMAC-SHA256
     * signature using the server-side secret before returning any claims.
     * A tampered payload or an arbitrary signature causes a SignatureException
     * to be thrown immediately.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
