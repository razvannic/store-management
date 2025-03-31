package local.dev.storemanager.application.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

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
}
