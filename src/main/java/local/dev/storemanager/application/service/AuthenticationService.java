package local.dev.storemanager.application.service;

import org.springframework.stereotype.Service;

import static local.dev.storemanager.domain.model.UserRoles.ADMIN;
import static local.dev.storemanager.domain.model.UserRoles.USER;

@Service
public class AuthenticationService {

    public String resolveRole(String username, String password) {
        return switch (username) {
            case "admin" -> password.equals("admin") ? ADMIN : null;
            case "user" -> password.equals("user") ? USER : null;
            default -> null;
        };
    }
}
