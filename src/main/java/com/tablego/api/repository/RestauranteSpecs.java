package com.tablego.api.repository;

import org.springframework.data.jpa.domain.Specification;

import com.tablego.api.model.Restaurante;

public final class RestauranteSpecs {
    private RestauranteSpecs() {}

    private static Specification<Restaurante> none() { return (r, q, cb) -> cb.conjunction(); }

    public static Specification<Restaurante> activo() {
        return (r, q, cb) -> cb.isTrue(r.get("activo"));
    }

    public static Specification<Restaurante> nombreContiene(String nombre) {
        if (nombre == null || nombre.isBlank()) return none();
        return (r, q, cb) -> cb.like(cb.lower(r.get("nombre")), "%" + nombre.trim().toLowerCase() + "%");
    }

    public static Specification<Restaurante> ciudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) return none();
        return (r, q, cb) -> cb.equal(cb.lower(r.get("ciudad")), ciudad.trim().toLowerCase());
    }

    /** Caja delimitadora (aprox.) alrededor del punto: eficiente y sin funciones SQL especiales. */
    public static Specification<Restaurante> cercaDe(Double lat, Double lng, Double radioKm) {
        if (lat == null || lng == null || radioKm == null) return none();
        double dLat = radioKm / 111.0;
        double dLng = radioKm / (111.0 * Math.max(Math.cos(Math.toRadians(lat)), 0.01));
        return (r, q, cb) -> cb.and(
                cb.between(r.<Double>get("latitud"), lat - dLat, lat + dLat),
                cb.between(r.<Double>get("longitud"), lng - dLng, lng + dLng));
    }
}
