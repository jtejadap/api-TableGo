package com.tablego.api.service;


import com.tablego.api.dto.CurrentUser;
import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.Paging;
import com.tablego.api.dto.ProductoDtos.*;
import com.tablego.api.exception.ResourceNotFoundException;
import com.tablego.api.model.CategoriaProducto;
import com.tablego.api.model.Producto;
import com.tablego.api.model.Restaurante;
import com.tablego.api.repository.ProductoRepository;
import com.tablego.api.repository.ProductoSpecs;
import com.tablego.api.repository.RestauranteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repo;
    private final RestauranteRepository restauranteRepo;
    private final CurrentUser currentUser;

    /** Menú visible para el cliente. */
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listarMenu(Long restauranteId, String nombre, CategoriaProducto categoria,
                                                     Boolean disponible, int page, int size) {
        if (!restauranteRepo.existsById(restauranteId)) throw new ResourceNotFoundException("Restaurante no encontrado");
        return listar(restauranteId, nombre, categoria, disponible, page, size);
    }

    /** Catálogo del restaurante del administrador. */
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listarPropios(String nombre, CategoriaProducto categoria,
                                                        Boolean disponible, int page, int size) {
        return listar(currentUser.restauranteDelStaff().getId(), nombre, categoria, disponible, page, size);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        Restaurante rest = currentUser.restauranteDelStaff();
        Producto p = Producto.builder().restaurante(rest).nombre(req.nombre()).descripcion(req.descripcion())
                .precio(req.precio()).categoria(req.categoria())
                .disponible(req.disponible() == null || req.disponible()).build();
        return toResponse(repo.save(p));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto p = propio(id);
        p.setNombre(req.nombre());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setCategoria(req.categoria());
        if (req.disponible() != null) p.setDisponible(req.disponible());
        return toResponse(p);
    }

    @Transactional
    public ProductoResponse cambiarDisponibilidad(Long id, boolean disponible) {
        Producto p = propio(id);
        p.setDisponible(disponible);
        return toResponse(p);
    }

    @Transactional
    public void eliminar(Long id) {
        Producto p = propio(id);
        p.setActivo(false);
        p.setDisponible(false);
    }

    private PageResponse<ProductoResponse> listar(Long restauranteId, String nombre, CategoriaProducto categoria,
                                                  Boolean disponible, int page, int size) {
        Specification<Producto> spec = Specification.allOf(
                ProductoSpecs.delRestaurante(restauranteId),
                ProductoSpecs.activo(),
                ProductoSpecs.nombreContiene(nombre),
                ProductoSpecs.categoria(categoria),
                ProductoSpecs.disponible(disponible));
        return PageResponse.of(repo.findAll(spec, Paging.of(page, size, Sort.by("categoria", "nombre")))
                .map(this::toResponse));
    }

    private Producto propio(Long id) {
        return repo.findByIdAndRestauranteIdAndActivoTrue(id, currentUser.restauranteDelStaff().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.getCategoria(), p.isDisponible());
    }
}
