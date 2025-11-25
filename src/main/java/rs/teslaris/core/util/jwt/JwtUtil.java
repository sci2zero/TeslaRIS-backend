package rs.teslaris.core.util.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.user.JwtToken;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.JwtTokenRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
    private final SecureRandom secureRandom = new SecureRandom();

    private final JwtTokenRepository jwtTokenRepository;

    @Value("${jwt.token.validity}")
    public Long tokenValidity;

    @Value("${jwt.signing.key}")
    public String signingKey;

    @Value("${spring.application.name}")
    public String appName;


    public String extractUsernameFromToken(String token) {
        if (token.startsWith("Bearer")) {
            token = parseJWTFromHeader(token);
        }

        return extractClaim(token, Claims::getSubject);
    }

    public String extractJtiFromToken(String token) {
        if (token.startsWith("Bearer")) {
            token = parseJWTFromHeader(token);
        }

        return extractClaim(token, Claims::getId);
    }

    public Integer extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer")) {
            token = parseJWTFromHeader(token);
        }

        var claims = this.getAllClaimsFromToken(token);
        return claims.get("userId", Integer.class);
    }

    public String extractUserRoleFromToken(String token) {
        if (token.startsWith("Bearer")) {
            token = parseJWTFromHeader(token);
        }

        var claims = this.getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    public String extractJWTSecurity(String token) {
        if (token.startsWith("Bearer")) {
            token = parseJWTFromHeader(token);
        }

        var claims = this.getAllClaimsFromToken(token);
        return claims.get("jwt-security-fingerprint", String.class);
    }

    public Date extractExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final var claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private String parseJWTFromHeader(String bearerToken) {
        return bearerToken.split(" ")[1];
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(signingKey)
            .parseClaimsJws(token)
            .getBody();
    }

    private Boolean isTokenExpired(String token) {
        final var expiration = extractExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String generateToken(Authentication authentication, String fingerprint) {
        String jwtSecurityHash = this.generateJWTSecurityFingerprintHash(fingerprint);
        var user = (User) authentication.getPrincipal();

        return performGeneration(authentication.getName(), jwtSecurityHash, user);
    }

    public String generateToken(UserDetails userDetails, String fingerprint) {
        String jwtSecurityHash = this.generateJWTSecurityFingerprintHash(fingerprint);
        var user = (User) userDetails;

        return performGeneration(user.getEmail(), jwtSecurityHash, user);
    }

    private String performGeneration(String email, String jwtSecurityHash, User user) {
        String jti = UUID.randomUUID().toString();
        var now = new Date(System.currentTimeMillis());
        var expiration = new Date(System.currentTimeMillis() + tokenValidity);
        var userRole = user.getAuthority().getName();

        var jwt = Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setId(jti)
            .setIssuer(appName)
            .setSubject(email)
            .claim("jwt-security-fingerprint", jwtSecurityHash)
            .claim("role", userRole)
            .claim("userId", user.getId())
            .claim("uiLang", user.getPreferredUILanguage().getLanguageTag().toLowerCase())
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(signatureAlgorithm, signingKey)
            .compact();


        // Allow parrallel sessions for sysadmins and commissions, otherwise don't
        if (!userRole.equals(UserRole.ADMIN.name()) &&
            !userRole.equals(UserRole.COMMISSION.name())) {
            jwtTokenRepository.deleteForUser(user.getId());
        }

        jwtTokenRepository.save(
            new JwtToken(jti, user, now.toInstant(), expiration.toInstant(), false));

        return jwt;
    }

    public String generateJWTSecurityFingerprint() {
        // Random string generation, which acts as a fingerprint for user
        byte[] randomFgp = new byte[50];
        this.secureRandom.nextBytes(randomFgp);
        return DatatypeConverter.printHexBinary(randomFgp);
    }

    private String generateJWTSecurityFingerprintHash(String jwtSecurity) {
        // Generating a hash for the fingerprint that we put into a token
        // (to prevent XSS attacker from reading the fingerprint and setting
        // the expected cookie on its own)
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] userFingerprintDigest =
                digest.digest(jwtSecurity.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(userFingerprintDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public boolean checkAlgHeaderParam(String token) {
        var algorithm = Jwts.parser()
            .setSigningKey(signingKey)
            .parseClaimsJws(token)
            .getHeader()
            .getAlgorithm();
        return algorithm.equals(signatureAlgorithm.getValue());
    }

    public Boolean validateToken(String token, UserDetails userDetails, String cookieValue) {
        final String username = extractUsernameFromToken(token);
        String jwtSecurityHash = this.generateJWTSecurityFingerprintHash(cookieValue);
        String jwtSecurity = this.extractJWTSecurity(token);

        final String jti = extractJtiFromToken(token);
        var tokenOpt = jwtTokenRepository.findByJti(jti);

        if (tokenOpt.isEmpty() || tokenOpt.get().isRevoked()) {
            return false;
        }

        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token)) &&
            jwtSecurity.equals(jwtSecurityHash);
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public void revokeToken(Integer userId) {
        jwtTokenRepository.findByUserIdAndRevokedFalse(userId).forEach(token -> {
            token.setRevoked(true);
            jwtTokenRepository.save(token);
        });
    }

    public void revokeToken(String jti) {
        jwtTokenRepository.findByJti(jti).ifPresent(token -> {
            token.setRevoked(true);
            jwtTokenRepository.save(token);
        });
    }

    public void revokeAllNonAdminTokens() {
        jwtTokenRepository.findAll().forEach(token -> {
            if (token.getUser().getAuthority().getName().equals(UserRole.ADMIN.name())) {
                return;
            }

            token.setRevoked(true);
            jwtTokenRepository.save(token);
        });
    }

    public void cleanupExpiredTokens() {
        jwtTokenRepository.deleteRevokedAndExpiredTokens();
    }
}
