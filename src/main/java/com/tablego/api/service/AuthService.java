package com.tablego.api.service;

import com.tablego.api.model.Role;
import com.tablego.api.model.User;
import com.tablego.api.repository.UserRepository;
import com.tablego.api.dto.AuthResponse;
import com.tablego.api.dto.LoginRequest;
import com.tablego.api.dto.RegisterRequest;
import com.tablego.api.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** El registro publico siempre crea usuarios con rol CLIENTE. */
    public AuthResponse register(RegisterRequest request) {
        User user = userService.create(request.nombre(), request.email(), request.password(), Role.CLIENTE);
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (!user.isActivo()) {
            throw new DisabledException("La cuenta está deshabilitada");
        }
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        JwtService.TokenInfo token = jwtService.generateToken(user);
        return new AuthResponse(token.token(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }
}
