package local.dev.storemanager.infrastructure.rest;

import local.dev.storemanager.application.dto.LoginRequestDto;
import local.dev.storemanager.application.dto.LoginResponseDto;
import local.dev.storemanager.application.security.JwtUtil;
import local.dev.storemanager.application.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthenticationService authenticationService;

    public AuthController(JwtUtil jwtUtil, AuthenticationService authenticationService) {
        this.jwtUtil = jwtUtil;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        var role = authenticationService.resolveRole(request.username(), request.password());
        if (role != null) {
            final var token = jwtUtil.generateToken(request.username(), role);
            final var response = new LoginResponseDto(
                    token,
                    "Bearer",
                    jwtUtil.getExpirationMillis()
            );
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
}
