package com.tablego.api.repository;

import org.springframework.data.jpa.domain.Specification;

import com.tablego.api.model.CategoriaProducto;
import com.tablego.api.model.Producto;

public final class ProductoSpecs {
    private ProductoSpecs() {}

    private static Specification<Producto> none() { return (p, q, cb) -> cb.conjunction(); }

    public static Specification<Producto> delRestaurante(Long restauranteId) {
        return (p, q, cb) -> cb.equal(p.get("restaurante").get("id"), restauranteId);
    }

    public static Specification<Producto> activo() {
        return (p, q, cb) -> cb.isTrue(p.get("activo"));
    }

    public static Specification<Producto> nombreContiene(String nombre) {
        if (nombre == null || nombre.isBlank()) return none();
        return (p, q, cb) -> cb.like(cb.lower(p.get("nombre")), "%" + nombre.trim().toLowerCase() + "%");
    }

    public static Specification<Producto> categoria(CategoriaProducto c) {
        if (c == null) return none();
        return (p, q, cb) -> cb.equal(p.get("categoria"), c);
    }

    public static Specification<Producto> disponible(Boolean d) {
        if (d == null) return none();
        return (p, q, cb) -> cb.equal(p.get("disponible"), d);
    }
}
