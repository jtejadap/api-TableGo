package com.tablego.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.tablego.api.model.EstadoReserva;

public final class ReservaDtos {
    private ReservaDtos() {}

    public record ItemRequest(
            @NotNull Long productoId,
            @NotNull @Min(1) @Max(50) Integer cantidad,
            @Size(max = 200) String nota) {}

    public record CrearReservaRequest(
            @NotNull Long restauranteId,
            @NotNull @Future LocalDateTime fechaHora,
            @NotNull @Min(1) @Max(50) Integer numPersonas,
            @Size(max = 500) String notas,
            @Valid List<ItemRequest> items) {}

    public record ActualizarPedidoRequest(@NotNull @Valid List<ItemRequest> items) {}

    public record CambiarEstadoRequest(@NotNull EstadoReserva estado, Long mesaId) {}

    public record ItemResponse(Long productoId, String producto, Integer cantidad,
                               BigDecimal precioUnitario, BigDecimal subtotal, String nota) {}

    public record ReservaResponse(
            Long id, Long restauranteId, String restaurante, Long clienteId, String cliente,
            Long mesaId, Integer mesaNumero, LocalDateTime fechaHora, Integer numPersonas,
            EstadoReserva estado, String notas, List<ItemResponse> items, BigDecimal total) {}
}
