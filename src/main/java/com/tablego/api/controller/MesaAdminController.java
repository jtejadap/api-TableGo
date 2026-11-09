package com.tablego.api.controller;

import com.tablego.api.dto.PageResponse;
import com.tablego.api.service.MesaService;
import com.tablego.api.dto.MesaDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mesas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class MesaAdminController {

    private final MesaService service;

    @GetMapping
    public PageResponse<MesaResponse> listar(
            @RequestParam(required = false) Integer capacidadMin,
            @RequestParam(required = false) Boolean activa,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.listar(capacidadMin, activa, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MesaResponse crear(@Valid @RequestBody MesaRequest req) { return service.crear(req); }

    @PutMapping("/{id}")
    public MesaResponse actualizar(@PathVariable Long id, @Valid @RequestBody MesaRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) { service.desactivar(id); }
}
