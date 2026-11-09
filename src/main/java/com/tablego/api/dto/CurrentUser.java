package com.tablego.api.dto;

import com.tablego.api.exception.BusinessRuleException;
import com.tablego.api.exception.ResourceNotFoundException;
import com.tablego.api.model.Restaurante;
import com.tablego.api.model.User;
import com.tablego.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * componente usado para obtener el usuario autenticado y su restaurante (si es staff)
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    /** Restaurante del staff (recepcionista/administrador) autenticado. */
    public Restaurante restauranteDelStaff() {
        Restaurante r = get().getRestaurante();
        if (r == null) throw new BusinessRuleException("El usuario no tiene un restaurante asignado");
        return r;
    }
}
