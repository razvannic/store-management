package local.dev.storemanager.application.service.users;

import local.dev.storemanager.infrastructure.persistence.jparepository.UserJpaRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UsersService implements UserDetailsService {

    private final UserJpaRepository userJpaRepository;

    public UsersService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userJpaRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().replace("ROLE_", "")) // Spring only recognises roles without the prefix
                .build();
    }
}