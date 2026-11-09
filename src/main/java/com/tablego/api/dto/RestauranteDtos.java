package com.tablego.api.dto;

import jakarta.validation.constraints.*;

import java.time.LocalTime;

public final class RestauranteDtos {
    private RestauranteDtos() {}

    public record RestauranteRequest(
            @NotBlank @Size(max = 150) String nombre,
            @Size(max = 1000) String descripcion,
            @NotBlank @Size(max = 200) String direccion,
            @NotBlank @Size(max = 100) String ciudad,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
            @Size(max = 30) String telefono,
            @NotNull LocalTime horaApertura,
            @NotNull LocalTime horaCierre,
            @Min(30) @Max(480) Integer duracionReservaMin) {}

    public record RestauranteResponse(
            Long id, String nombre, String descripcion, String direccion, String ciudad,
            Double latitud, Double longitud, String telefono,
            LocalTime horaApertura, LocalTime horaCierre, int duracionReservaMin,
            Double distanciaKm) {}
}
