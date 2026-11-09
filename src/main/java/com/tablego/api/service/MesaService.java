package com.tablego.api.service;

import com.tablego.api.dto.CurrentUser;
import com.tablego.api.dto.MesaDtos.*;
import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.Paging;
import com.tablego.api.exception.BusinessRuleException;
import com.tablego.api.exception.ResourceNotFoundException;
import com.tablego.api.model.Mesa;
import com.tablego.api.model.Restaurante;
import com.tablego.api.repository.MesaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository repo;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public PageResponse<MesaResponse> listar(Integer capacidadMin, Boolean activa, int page, int size) {
        Long rid = currentUser.restauranteDelStaff().getId();
        Specification<Mesa> spec = (m, q, cb) -> cb.equal(m.get("restaurante").get("id"), rid);
        if (capacidadMin != null) spec = spec.and((m, q, cb) -> cb.greaterThanOrEqualTo(m.get("capacidad"), capacidadMin));
        if (activa != null) spec = spec.and((m, q, cb) -> cb.equal(m.get("activa"), activa));
        return PageResponse.of(repo.findAll(spec, Paging.of(page, size, Sort.by("numero"))).map(this::toResponse));
    }

    @Transactional
    public MesaResponse crear(MesaRequest req) {
        Restaurante rest = currentUser.restauranteDelStaff();
        if (repo.existsByRestauranteIdAndNumero(rest.getId(), req.numero())) {
            throw new BusinessRuleException("Ya existe la mesa número " + req.numero());
        }
        Mesa m = Mesa.builder().restaurante(rest).numero(req.numero()).capacidad(req.capacidad())
                .activa(req.activa() == null || req.activa()).build();
        return toResponse(repo.save(m));
    }

    @Transactional
    public MesaResponse actualizar(Long id, MesaRequest req) {
        Mesa m = propia(id);
        if (!m.getNumero().equals(req.numero())
                && repo.existsByRestauranteIdAndNumero(m.getRestaurante().getId(), req.numero())) {
            throw new BusinessRuleException("Ya existe la mesa número " + req.numero());
        }
        m.setNumero(req.numero());
        m.setCapacidad(req.capacidad());
        if (req.activa() != null) m.setActiva(req.activa());
        return toResponse(m);
    }

    /** Baja lógica: la mesa puede estar referenciada por reservas históricas. */
    @Transactional
    public void desactivar(Long id) {
        propia(id).setActiva(false);
    }

    private Mesa propia(Long id) {
        return repo.findByIdAndRestauranteId(id, currentUser.restauranteDelStaff().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));
    }

    private MesaResponse toResponse(Mesa m) {
        return new MesaResponse(m.getId(), m.getNumero(), m.getCapacidad(), m.isActiva());
    }
}
