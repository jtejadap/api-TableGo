package com.tablego.api.model;

/**
 * PENDIENTE -> CONFIRMADA -> EN_CURSO -> COMPLETADA
 *     |            |-> NO_SHOW
 *     |-> CANCELADA <-|
 */
public enum EstadoReserva {
    PENDIENTE, CONFIRMADA, EN_CURSO, COMPLETADA, CANCELADA, NO_SHOW;

    public boolean puedePasarA(EstadoReserva destino) {
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == EN_CURSO || destino == CANCELADA || destino == NO_SHOW;
            case EN_CURSO -> destino == COMPLETADA;
            default -> false;
        };
    }

    /** El cliente aún puede modificar su pedido. */
    public boolean editable() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
