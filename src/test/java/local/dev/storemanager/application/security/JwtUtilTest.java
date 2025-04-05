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
        final var issuedAt = System.currentTimeMillis();
        final var token = jwtUtil.generateToken("admin", "ROLE_ADMIN");

        final var claims = jwtUtil.extractClaims(token);
        final var actualExp = claims.getExpiration().getTime();
        final var expectedExp = issuedAt + expiration;

        final var tolerance = 2000;
        assertTrue(Math.abs(actualExp - expectedExp) <= tolerance,
                "Token expiration should match the configured value");
    }

}
