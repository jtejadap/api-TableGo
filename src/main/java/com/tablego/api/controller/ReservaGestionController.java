package com.tablego.api.controller;

import com.tablego.api.dto.MesaDtos.MesaResponse;
import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.ReservaDtos.*;
import com.tablego.api.model.EstadoReserva;
import com.tablego.api.service.MesaService;
import com.tablego.api.service.ReservaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Gestión de reservas del restaurante del staff autenticado. */
@RestController
@RequestMapping("/api/gestion/reservas")
@PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMINISTRADOR')")
@RequiredArgsConstructor
public class ReservaGestionController {

    private final ReservaService service;
    private final MesaService mesas;   

    /** Ej: /api/gestion/reservas?estado=PENDIENTE&desde=2026-11-05&hasta=2026-11-05&nombreCliente=ana */
    @GetMapping
    public PageResponse<ReservaResponse> listar(
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String nombreCliente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarDelRestaurante(estado, desde, hasta, nombreCliente, page, size);
    }

    @GetMapping("/mesas")
    public PageResponse<MesaResponse> listar(
            @RequestParam(required = false) Integer capacidadMin,
            @RequestParam(required = false) Boolean activa,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return mesas.listar(capacidadMin, activa, page, size);
    }

    @PatchMapping("/{id}/estado")
    public ReservaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest req) {
        return service.cambiarEstado(id, req);
    }
}
