package fi.haagahelia.messenger.service;

import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import fi.haagahelia.messenger.dto.RegisterUserDto;
import fi.haagahelia.messenger.model.User;
import fi.haagahelia.messenger.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !(authentication instanceof UsernamePasswordAuthenticationToken)) {
            return Optional.empty();
        }

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;

        if (token.getPrincipal() == null) {
            return Optional.empty();
        }

        return userRepository.findOneByUsername(token.getPrincipal().toString());
    }

    public User registerUser(RegisterUserDto registration) {
        String passwordHash = passwordEncoder.encode(registration.password());
        User newUser = new User(registration.username(), passwordHash, "USER");

        return userRepository.save(newUser);
    }
}
