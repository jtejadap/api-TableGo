package com.tablego.api.controller;

import com.tablego.api.dto.RestauranteDtos.*;
import com.tablego.api.service.RestauranteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/restaurante")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class RestauranteAdminController {

    private final RestauranteService service;

    @GetMapping
    public RestauranteResponse miRestaurante() { return service.miRestaurante(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestauranteResponse crear(@Valid @RequestBody RestauranteRequest req) { return service.crear(req); }

    @PutMapping
    public RestauranteResponse actualizar(@Valid @RequestBody RestauranteRequest req) { return service.actualizar(req); }
}
