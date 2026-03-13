package fi.haagahelia.messenger.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fi.haagahelia.messenger.dto.AccessTokenPayloadDto;
import fi.haagahelia.messenger.dto.LoginUserDto;
import fi.haagahelia.messenger.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Operations for authentication")
public class AuthRestController {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthRestController(JwtService jwtService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Operation(summary = "Authenticate a user", description = "Authenticates the user and returns an access token upon a successful authentication")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginUserDto login, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        UsernamePasswordAuthenticationToken credentials = new UsernamePasswordAuthenticationToken(login.username(),
                login.password());

        try {
            Authentication auth = authenticationManager.authenticate(credentials);
            AccessTokenPayloadDto accessTokenPayload = jwtService.getAccessToken(auth.getName());

            return ResponseEntity.ok().body(accessTokenPayload);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid username or password");
        }

    }
}
