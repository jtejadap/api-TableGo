package com.tablego.api.controller;

import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.ProductoDtos.ProductoResponse;
import com.tablego.api.model.CategoriaProducto;
import com.tablego.api.service.ProductoService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Menú de un restaurante (cualquier usuario autenticado). */
@RestController
@RequestMapping("/api/restaurantes/{restauranteId}/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @GetMapping
    public PageResponse<ProductoResponse> menu(
            @PathVariable Long restauranteId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) CategoriaProducto categoria,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarMenu(restauranteId, nombre, categoria, disponible, page, size);
    }
}
