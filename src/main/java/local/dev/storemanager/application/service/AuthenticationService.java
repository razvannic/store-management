package local.dev.storemanager.application.service;

import local.dev.storemanager.application.dto.LoginRequestDto;
import local.dev.storemanager.application.dto.LoginResponseDto;
import local.dev.storemanager.application.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import local.dev.storemanager.infrastructure.persistence.jparepository.UserJpaRepository;

@Service
public class AuthenticationService {

    private final UserJpaRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserJpaRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDto authenticate(LoginRequestDto request) {
        var user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        // using encoder as not using Spring Security login yet
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new LoginResponseDto(token, "Bearer", jwtUtil.getExpirationMillis());
    }
}
