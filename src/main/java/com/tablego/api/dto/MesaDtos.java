package com.tablego.api.dto;

import jakarta.validation.constraints.*;

public final class MesaDtos {
    private MesaDtos() {}

    public record MesaRequest(
            @NotNull @Min(1) Integer numero,
            @NotNull @Min(1) @Max(50) Integer capacidad,
            Boolean activa) {}

    public record MesaResponse(Long id, Integer numero, Integer capacidad, boolean activa) {}
}
