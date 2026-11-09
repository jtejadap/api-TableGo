package com.tablego.api.controller;

import com.tablego.api.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints SOLO para probar autenticacion/autorizacion y el manejo de errores.
 * Se pueden borrar cuando ya existan los endpoints reales.
 */
@RestController
@RequestMapping("/api")
public class PruebaController {

    // Publico (sin token)
    @GetMapping("/public/ping")
    public Map<String, String> publico() {
        return Map.of("mensaje", "Endpoint público: no requiere token");
    }

    // Provoca un 404 de negocio para ver el formato de error
    @GetMapping("/public/error-test")
    public Map<String, String> errorTest() {
        throw new ResourceNotFoundException("Recurso de prueba no encontrado");
    }

    // Cualquier usuario autenticado
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "email", jwt.getSubject(),
                "uid", jwt.getClaim("uid"),
                "roles", jwt.getClaimAsStringList("roles"),
                "expiraEn", String.valueOf(jwt.getExpiresAt()));
    }

    @GetMapping("/cliente/ping")
    @PreAuthorize("hasRole('CLIENTE')")
    public Map<String, String> cliente(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("mensaje", "Acceso de CLIENTE", "usuario", jwt.getSubject());
    }

    @GetMapping("/recepcion/ping")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public Map<String, String> recepcion(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("mensaje", "Acceso de RECEPCIONISTA", "usuario", jwt.getSubject());
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, String> admin(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("mensaje", "Acceso de ADMINISTRADOR", "usuario", jwt.getSubject());
    }

    // Ejemplo de endpoint compartido por varios roles
    @GetMapping("/staff/ping")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')")
    public Map<String, String> staff(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("mensaje", "Acceso de personal (RECEPCIONISTA o ADMINISTRADOR)", "usuario", jwt.getSubject());
    }
}
