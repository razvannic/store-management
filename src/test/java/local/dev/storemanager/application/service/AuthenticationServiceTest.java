package local.dev.storemanager.application.service;

import local.dev.storemanager.application.dto.LoginRequestDto;
import local.dev.storemanager.application.dto.LoginResponseDto;
import local.dev.storemanager.application.security.JwtUtil;
import local.dev.storemanager.infrastructure.persistence.entity.AppUser;
import local.dev.storemanager.infrastructure.persistence.jparepository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder();
        authenticationService = new AuthenticationService(userRepository, jwtUtil, passwordEncoder);
    }

    @Test
    void shouldAuthenticateSuccessfully() {
        String rawPassword = "admin";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        var user = AppUser.builder()
                .id(1L)
                .username("admin")
                .password(encodedPassword)
                .role("ROLE_ADMIN")
                .build();

        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("admin", "ROLE_ADMIN")).thenReturn("mocked-token");
        when(jwtUtil.getExpirationMillis()).thenReturn(3600L);

        LoginRequestDto request = new LoginRequestDto("admin", "admin");
        LoginResponseDto response = authenticationService.authenticate(request);

        assertEquals("mocked-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
    }

    @Test
    void shouldThrowIfUsernameNotFound() {
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        LoginRequestDto request = new LoginRequestDto("ghost", "admin");

        assertThrows(UsernameNotFoundException.class, () ->
                authenticationService.authenticate(request));
    }

    @Test
    void shouldThrowIfPasswordDoesNotMatch() {
        String encodedPassword = passwordEncoder.encode("admin");

        var user = AppUser.builder()
                .id(1L)
                .username("admin")
                .password(encodedPassword)
                .role("ROLE_ADMIN")
                .build();

        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));

        LoginRequestDto request = new LoginRequestDto("admin", "wrong-password");

        assertThrows(BadCredentialsException.class, () ->
                authenticationService.authenticate(request));
    }

}

