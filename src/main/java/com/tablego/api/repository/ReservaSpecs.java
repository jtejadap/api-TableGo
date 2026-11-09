package com.tablego.api.repository;

import org.springframework.data.jpa.domain.Specification;

import com.tablego.api.model.EstadoReserva;
import com.tablego.api.model.Reserva;

import java.time.LocalDate;

public final class ReservaSpecs {
    private ReservaSpecs() {}

    private static Specification<Reserva> none() { return (r, q, cb) -> cb.conjunction(); }

    public static Specification<Reserva> delCliente(Long clienteId) {
        return (r, q, cb) -> cb.equal(r.get("cliente").get("id"), clienteId);
    }

    public static Specification<Reserva> delRestaurante(Long restauranteId) {
        return (r, q, cb) -> cb.equal(r.get("restaurante").get("id"), restauranteId);
    }

    public static Specification<Reserva> estado(EstadoReserva estado) {
        if (estado == null) return none();
        return (r, q, cb) -> cb.equal(r.get("estado"), estado);
    }

    public static Specification<Reserva> desde(LocalDate d) {
        if (d == null) return none();
        return (r, q, cb) -> cb.greaterThanOrEqualTo(r.get("fechaHora"), d.atStartOfDay());
    }

    public static Specification<Reserva> hasta(LocalDate d) {
        if (d == null) return none();
        return (r, q, cb) -> cb.lessThan(r.get("fechaHora"), d.plusDays(1).atStartOfDay());
    }

    public static Specification<Reserva> nombreCliente(String nombre) {
        if (nombre == null || nombre.isBlank()) return none();
        return (r, q, cb) -> cb.like(cb.lower(r.get("cliente").get("nombre")), "%" + nombre.trim().toLowerCase() + "%");
    }
}
