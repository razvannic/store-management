package local.dev.storemanager.infrastructure.rest;

import local.dev.storemanager.application.security.JwtUtil;
import local.dev.storemanager.application.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class JwtUtilTestConfig {
        @Bean
        public JwtUtil jwtUtil() {
            JwtUtil mock = Mockito.mock(JwtUtil.class);
            when(mock.generateToken("admin", "ROLE_ADMIN")).thenReturn("mocked-token");
            when(mock.getExpirationMillis()).thenReturn(3600L);
            return mock;
        }

        @Bean
        public AuthenticationService authenticationService() {
            AuthenticationService mock = Mockito.mock(AuthenticationService.class);
            when(mock.resolveRole("admin", "admin")).thenReturn("ROLE_ADMIN");
            when(mock.resolveRole("wrong", "wrong")).thenReturn(null);
            return mock;
        }

        // Overriding security for this test because real Spring Security config is still active in the test,
        // and it's applying its full JWT logic even in @WebMvcTest
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    );
            return http.build();
        }
    }

    @Test
    void shouldReturnTokenForValidAdminLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "username": "admin", "password": "admin" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600L));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "username": "wrong", "password": "wrong" }
                        """))
                .andExpect(status().isUnauthorized());
    }
}
