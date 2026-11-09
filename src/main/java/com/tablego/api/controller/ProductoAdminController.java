package com.tablego.api.controller;

import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.ProductoDtos.*;
import com.tablego.api.model.CategoriaProducto;
import com.tablego.api.service.ProductoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/productos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class ProductoAdminController {

    private final ProductoService service;

    @GetMapping
    public PageResponse<ProductoResponse> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) CategoriaProducto categoria,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.listarPropios(nombre, categoria, disponible, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest req) { return service.crear(req); }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/disponibilidad")
    public ProductoResponse disponibilidad(@PathVariable Long id, @RequestParam boolean valor) {
        return service.cambiarDisponibilidad(id, valor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
