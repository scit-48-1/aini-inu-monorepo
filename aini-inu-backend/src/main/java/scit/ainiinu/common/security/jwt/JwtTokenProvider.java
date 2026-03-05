package scit.ainiinu.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 Provider
 *
 * - Access Token: 1시간 (짧은 TTL로 보안 강화)
 * - Refresh Token: 14일 (사용자 편의성)
 * - Algorithm: HMAC-SHA256
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;  // 3,600,000ms = 1시간
    private final long refreshTokenValidityMs; // 1,209,600,000ms = 14일

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms:3600000}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms:1209600000}") long refreshTokenValidityMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /**
     * Access Token 생성
     *
     * @param memberId 회원 ID
     * @return JWT Access Token (만료: 1시간)
     */
    public String generateAccessToken(Long memberId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성
     *
     * @param memberId 회원 ID
     * @return JWT Refresh Token (만료: 14일)
     */
    public String generateRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenValidityMs);

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT 토큰 검증 및 memberId 추출
     *
     * @param token JWT 토큰
     * @return 회원 ID
     * @throws BusinessException C102 (INVALID_TOKEN) - 잘못된 토큰
     * @throws BusinessException C103 (EXPIRED_TOKEN) - 만료된 토큰
     */
    public Long validateAndGetMemberId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.parseLong(claims.getSubject());

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            throw new BusinessException(CommonErrorCode.EXPIRED_TOKEN);

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new BusinessException(CommonErrorCode.INVALID_TOKEN);
        }
    }
}
