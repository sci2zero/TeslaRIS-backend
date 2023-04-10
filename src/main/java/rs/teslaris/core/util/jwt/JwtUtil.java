package rs.teslaris.core.util.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.function.Function;
import javax.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.User;

@Slf4j
@Component
public class JwtUtil {

    public static final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${jwt.token.validity}")
    public Long tokenValidity;
    @Value("${jwt.signing.key}")
    public String signingKey;
    @Value("${spring.application.name}")
    public String appName;

    public String extractUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Integer extractUserIdFromToken(String token) {
        var claims = this.getAllClaimsFromToken(token);
        return claims.get("userId", Integer.class);
    }

    public String extractJWTSecurity(String token) {
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

        return Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setIssuer(appName)
            .setSubject(authentication.getName())
            .claim("jwt-security-fingerprint", jwtSecurityHash)
            .claim("role", user.getAuthority().getName())
            .claim("userId", user.getId())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + tokenValidity))
            .signWith(signatureAlgorithm, signingKey)
            .compact();
    }

    public String generateToken(UserDetails userDetails, String fingerprint) {
        String jwtSecurityHash = this.generateJWTSecurityFingerprintHash(fingerprint);
        var user = (User) userDetails;

        return Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setIssuer(appName)
            .setSubject(user.getEmail())
            .claim("jwt-security-fingerprint", jwtSecurityHash)
            .claim("role", user.getAuthority().getName())
            .claim("userId", user.getId())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + tokenValidity))
            .signWith(signatureAlgorithm, signingKey)
            .compact();
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

        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token)) &&
            jwtSecurity.equals(jwtSecurityHash);
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
