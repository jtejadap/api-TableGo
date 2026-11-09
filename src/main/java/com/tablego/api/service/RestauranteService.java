package com.tablego.api.service;



import com.tablego.api.dto.CurrentUser;
import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.Paging;
import com.tablego.api.dto.RestauranteDtos.*;
import com.tablego.api.exception.BusinessRuleException;
import com.tablego.api.exception.ResourceNotFoundException;
import com.tablego.api.model.Restaurante;
import com.tablego.api.model.User;
import com.tablego.api.repository.RestauranteRepository;
import com.tablego.api.repository.RestauranteSpecs;
import com.tablego.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestauranteService {

    private final RestauranteRepository repo;
    private final UserRepository userRepo;
    private final CurrentUser currentUser;

    // ---------- cliente ----------

    @Transactional(readOnly = true)
    public PageResponse<RestauranteResponse> buscar(String nombre, String ciudad, Double lat, Double lng,
                                                    Double radioKm, int page, int size) {
        Specification<Restaurante> spec = Specification.allOf(
                RestauranteSpecs.activo(),
                RestauranteSpecs.nombreContiene(nombre),
                RestauranteSpecs.ciudad(ciudad),
                RestauranteSpecs.cercaDe(lat, lng, radioKm));
        Page<Restaurante> result = repo.findAll(spec, Paging.of(page, size, Sort.by("nombre")));
        return PageResponse.of(result.map(r -> toResponse(r, lat, lng)));
    }

    @Transactional(readOnly = true)
    public RestauranteResponse obtener(Long id) {
        return toResponse(repo.findById(id).filter(Restaurante::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante no encontrado")), null, null);
    }

    // ---------- administrador ----------

    @Transactional
    public RestauranteResponse crear(RestauranteRequest req) {
        User admin = currentUser.get();
        if (admin.getRestaurante() != null) {
            throw new BusinessRuleException("Ya tienes un restaurante asignado");
        }
        Restaurante r = repo.save(aplicar(new Restaurante(), req));
        admin.setRestaurante(r);
        userRepo.save(admin);
        return toResponse(r, null, null);
    }

    @Transactional(readOnly = true)
    public RestauranteResponse miRestaurante() {
        return toResponse(currentUser.restauranteDelStaff(), null, null);
    }

    @Transactional
    public RestauranteResponse actualizar(RestauranteRequest req) {
        Restaurante r = repo.findById(currentUser.restauranteDelStaff().getId()).orElseThrow();
        return toResponse(aplicar(r, req), null, null);
    }

    // ---------- helpers ----------

    private Restaurante aplicar(Restaurante r, RestauranteRequest req) {
        if (!req.horaApertura().isBefore(req.horaCierre())) {
            throw new BusinessRuleException("La hora de apertura debe ser anterior a la de cierre");
        }
        r.setNombre(req.nombre());
        r.setDescripcion(req.descripcion());
        r.setDireccion(req.direccion());
        r.setCiudad(req.ciudad());
        r.setLatitud(req.latitud());
        r.setLongitud(req.longitud());
        r.setTelefono(req.telefono());
        r.setHoraApertura(req.horaApertura());
        r.setHoraCierre(req.horaCierre());
        if (req.duracionReservaMin() != null) r.setDuracionReservaMin(req.duracionReservaMin());
        return r;
    }

    private RestauranteResponse toResponse(Restaurante r, Double lat, Double lng) {
        Double dist = (lat == null || lng == null) ? null
                : Math.round(haversineKm(lat, lng, r.getLatitud(), r.getLongitud()) * 100.0) / 100.0;
        return new RestauranteResponse(r.getId(), r.getNombre(), r.getDescripcion(), r.getDireccion(),
                r.getCiudad(), r.getLatitud(), r.getLongitud(), r.getTelefono(),
                r.getHoraApertura(), r.getHoraCierre(), r.getDuracionReservaMin(), dist);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
