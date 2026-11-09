package com.tablego.api.controller;

import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.RestauranteDtos.RestauranteResponse;
import com.tablego.api.service.RestauranteService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Consulta de restaurantes (cualquier usuario autenticado). */
@RestController
@RequestMapping("/api/restaurantes")
@RequiredArgsConstructor
public class RestauranteController {

    private final RestauranteService service;

    /** Ej: /api/restaurantes?lat=10.33&lng=-75.42&radioKm=5&nombre=pizza&ciudad=Cartagena&page=0&size=10 */
    @GetMapping
    public PageResponse<RestauranteResponse> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radioKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.buscar(nombre, ciudad, lat, lng, radioKm, page, size);
    }

    @GetMapping("/{id}")
    public RestauranteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
