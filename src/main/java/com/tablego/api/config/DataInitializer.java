package com.tablego.api.config;

import com.tablego.api.model.Role;
import com.tablego.api.repository.UserRepository;
import com.tablego.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario por rol para poder probar. Solo se ejecuta si app.seed.enabled=true.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        seed("Administrador", "admin@tablego.com", "Admin12345!", Role.ADMINISTRADOR);
        seed("Recepcionista", "recepcion@tablego.com", "Recepcion12345!", Role.RECEPCIONISTA);
        seed("Cliente", "cliente@tablego.com", "Cliente12345!", Role.CLIENTE);
    }

    private void seed(String nombre, String email, String password, Role role) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            userService.create(nombre, email, password, role);
            log.info("Usuario de prueba creado: {} ({})", email, role);
        }
    }
}
