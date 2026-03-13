package fi.haagahelia.messenger.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fi.haagahelia.messenger.dto.RegisterUserDto;
import fi.haagahelia.messenger.model.User;
import fi.haagahelia.messenger.repository.UserRepository;
import fi.haagahelia.messenger.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "User", description = "Operations for accessing and managing users")
public class UserRestController {
    private final UserRepository userRepository;
    private final UserService userService;

    public UserRestController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Operation(
        summary = "Get all users",
        description = "Returns all users"
    )
    @GetMapping("")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Operation(
        summary = "Create a user",
        description = "Creates a new user and returns the created user"
    )
    @PostMapping("")
    public User createUser(@Valid @RequestBody RegisterUserDto registration, BindingResult bindingResult) {
        Optional<User> existingUser = userRepository.findOneByUsername(registration.username());

        if (existingUser.isPresent()) {
            bindingResult.rejectValue("username", "UsernameTaken",
                    "This username is already taken. Choose another one");
        }

        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        return userService.registerUser(registration);
    }

     @Operation(
        summary = "Get the currently authenticated user",
        description = "Returns the currently authenticated user"
    )
    @GetMapping("/current")
    public User getCurrentUser() {
        return userService.getAuthenticatedUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
    }
}
