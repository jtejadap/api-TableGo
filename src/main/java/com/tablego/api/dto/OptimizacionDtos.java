package com.tablego.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** DTOs del modelo de Programación Lineal Entera para optimizar reservas. */
public final class OptimizacionDtos {
    private OptimizacionDtos() {}

    /** Datos de un tipo de reserva (grupos de 2, 4 o 6 personas). */
    public record GrupoRequest(
            @NotNull @DecimalMin(value = "0.0", inclusive = false) Double ticketPorPersona,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) Double duracionHoras,
            @NotNull @Min(0) Integer demandaMaxima) {}

    /** Datos que el administrador llena en el formulario. */
    public record OptimizacionRequest(
            @NotNull @Min(1) Integer aforo,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) Double horasFuncionamiento,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1.0") Double eficiencia,
            @NotNull @Min(0) Integer mesasDeSeis,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double costoVariable,
            @NotNull @DecimalMin("0.0") Double costosFijos,
            @NotNull @DecimalMin("0.0") Double proteinaDisponibleKg,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) Double consumoProteinaKg,
            @NotNull @Min(0) Integer reservasConfirmadasDeSeis,
            @NotNull @Valid GrupoRequest grupo2,
            @NotNull @Valid GrupoRequest grupo4,
            @NotNull @Valid GrupoRequest grupo6) {}

    /** Estado de una restriccion en la solucion optima. */
    public record RestriccionResponse(String codigo, String descripcion, String unidad,
                                      double usado, double limite, boolean saturada) {}

    /** Resultado que recibe el administrador. */
    public record OptimizacionResponse(String estado, boolean solucionOptima,
                                       int reservas2, int reservas4, int reservas6,
                                       int personasAtendidas, double contribucion,
                                       double costosFijos, double utilidadNeta,
                                       List<RestriccionResponse> restricciones, String mensaje) {}
}