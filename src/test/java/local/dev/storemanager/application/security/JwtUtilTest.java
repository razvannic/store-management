package local.dev.storemanager.application.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private long expiration;

    @Test
    void shouldGenerateAndValidateAdminToken() {
        final var token = jwtUtil.generateToken("adminUser", "ROLE_ADMIN");

        assertTrue(jwtUtil.validateToken(token));

        final var claims = jwtUtil.extractClaims(token);
        assertEquals("adminUser", claims.getSubject());
        assertEquals("ROLE_ADMIN", claims.get("role"));
    }

    @Test
    void shouldRejectTamperedToken() {
        final var token = jwtUtil.generateToken("user", "ROLE_USER");
        final var invalidToken = token + "garbage";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void shouldRejectCompletelyInvalidToken() {
        assertFalse(jwtUtil.validateToken("this.is.not.valid"));
    }

    @Test
    void shouldGenerateTokenWithCorrectExpirationTime() {
        long issuedAt = System.currentTimeMillis();
        String token = jwtUtil.generateToken("admin", "ROLE_ADMIN");

        Claims claims = jwtUtil.extractClaims(token);
        long actualExp = claims.getExpiration().getTime();
        long expectedExp = issuedAt + expiration;

        long tolerance = 2000;
        assertTrue(Math.abs(actualExp - expectedExp) <= tolerance,
                "Token expiration should match the configured value");
    }

}
