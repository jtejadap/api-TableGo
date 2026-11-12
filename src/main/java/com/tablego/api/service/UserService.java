package com.tablego.api.service;

import com.tablego.api.dto.CurrentUser;
import com.tablego.api.exception.BusinessRuleException;
import com.tablego.api.exception.ConflictException;
import com.tablego.api.model.Restaurante;
import com.tablego.api.model.Role;
import com.tablego.api.model.User;
import com.tablego.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;    
    private final CurrentUser currentUser;

    @Transactional
    public User create(String nombre, String email, String rawPassword, Role role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Ya existe un usuario con el email " + normalizedEmail);
        }
        User user = User.builder()
                .nombre(nombre.trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User createRecepcionist(String nombre, String email, String rawPassword) {
        User admin = currentUser.get();        
        if (admin.getRestaurante() == null) {
            throw new BusinessRuleException("Necesita crear un restaurante para crear cuentas de staff");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Ya existe un usuario con el email " + normalizedEmail);
        }       

        Restaurante r = currentUser.restauranteDelStaff();
        
        User user = User.builder()
                .nombre(nombre.trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.RECEPCIONISTA)
                .restaurante(r)
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
