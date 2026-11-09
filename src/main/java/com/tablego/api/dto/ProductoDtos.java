package com.tablego.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

import com.tablego.api.model.CategoriaProducto;

public final class ProductoDtos {
    private ProductoDtos() {}

    public record ProductoRequest(
            @NotBlank @Size(max = 150) String nombre,
            @Size(max = 500) String descripcion,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal precio,
            @NotNull CategoriaProducto categoria,
            Boolean disponible) {}

    public record ProductoResponse(Long id, String nombre, String descripcion, BigDecimal precio,
                                   CategoriaProducto categoria, boolean disponible) {}
}
