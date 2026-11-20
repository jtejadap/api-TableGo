package com.tablego.api.controller;

import com.tablego.api.dto.OptimizacionDtos.OptimizacionRequest;
import com.tablego.api.dto.OptimizacionDtos.OptimizacionResponse;
import com.tablego.api.service.OptimizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Optimizacion de reservas con Programacion Lineal Entera. Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/admin/optimizacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class OptimizacionController {

    private final OptimizacionService service;

    @PostMapping("/reservas")
    public OptimizacionResponse optimizar(@Valid @RequestBody OptimizacionRequest req) {
        return service.optimizar(req);
    }
}