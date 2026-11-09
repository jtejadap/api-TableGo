package com.tablego.api.controller;

import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.ReservaDtos.*;
import com.tablego.api.model.EstadoReserva;
import com.tablego.api.service.ReservaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Flujo del cliente: reservar, ver y ajustar su pedido, cancelar. */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService service;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody CrearReservaRequest req) { return service.crear(req); }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('CLIENTE')")
    public PageResponse<ReservaResponse> mias(
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.misReservas(estado, desde, hasta, page, size);
    }

    /** Dueño de la reserva o staff del restaurante. */
    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PutMapping("/{id}/pedido")
    @PreAuthorize("hasRole('CLIENTE')")
    public ReservaResponse actualizarPedido(@PathVariable Long id, @Valid @RequestBody ActualizarPedidoRequest req) {
        return service.actualizarPedido(id, req);
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ReservaResponse cancelar(@PathVariable Long id) { return service.cancelar(id); }
}
