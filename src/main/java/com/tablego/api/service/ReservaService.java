package com.tablego.api.service;


import com.tablego.api.model.EstadoReserva;
import com.tablego.api.model.Mesa;
import com.tablego.api.repository.MesaRepository;
import com.tablego.api.model.Producto;
import com.tablego.api.model.Reserva;
import com.tablego.api.model.ReservaItem;
import com.tablego.api.repository.ProductoRepository;
import com.tablego.api.repository.ReservaRepository;
import com.tablego.api.repository.ReservaSpecs;
import com.tablego.api.dto.CurrentUser;
import com.tablego.api.dto.PageResponse;
import com.tablego.api.dto.Paging;
import com.tablego.api.dto.ReservaDtos.*;
import com.tablego.api.exception.BusinessRuleException;
import com.tablego.api.exception.ResourceNotFoundException;
import com.tablego.api.model.Restaurante;
import com.tablego.api.repository.RestauranteRepository;
import com.tablego.api.model.Role;
import com.tablego.api.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final List<EstadoReserva> ESTADOS_QUE_OCUPAN_MESA =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    private final ReservaRepository repo;
    private final RestauranteRepository restauranteRepo;
    private final MesaRepository mesaRepo;
    private final ProductoRepository productoRepo;
    private final CurrentUser currentUser;

    // ===================== CLIENTE =====================

    @Transactional
    public ReservaResponse crear(CrearReservaRequest req) {
        User cliente = currentUser.get();
        Restaurante rest = restauranteRepo.findById(req.restauranteId()).filter(Restaurante::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante no encontrado"));

        LocalTime hora = req.fechaHora().toLocalTime();
        if (hora.isBefore(rest.getHoraApertura()) || hora.isAfter(rest.getHoraCierre())) {
            throw new BusinessRuleException("El restaurante atiende entre " + rest.getHoraApertura()
                    + " y " + rest.getHoraCierre());
        }
        if (!mesaRepo.existsByRestauranteIdAndActivaTrueAndCapacidadGreaterThanEqual(rest.getId(), req.numPersonas())) {
            throw new BusinessRuleException("El restaurante no tiene mesas para " + req.numPersonas() + " personas");
        }

        Reserva r = Reserva.builder().cliente(cliente).restaurante(rest)
                .fechaHora(req.fechaHora()).numPersonas(req.numPersonas()).notas(req.notas()).build();
        aplicarItems(r, req.items());
        return toResponse(repo.save(r));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservaResponse> misReservas(EstadoReserva estado, LocalDate desde, LocalDate hasta,
                                                     int page, int size) {
        Specification<Reserva> spec = Specification.allOf(
                ReservaSpecs.delCliente(currentUser.get().getId()),
                ReservaSpecs.estado(estado), ReservaSpecs.desde(desde), ReservaSpecs.hasta(hasta));
        return PageResponse.of(repo.findAll(spec, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "fechaHora")))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long id) {
        return toResponse(cargarConAcceso(id));
    }

    @Transactional
    public ReservaResponse actualizarPedido(Long id, ActualizarPedidoRequest req) {
        Reserva r = propiaDelCliente(id);
        if (!r.getEstado().editable()) {
            throw new BusinessRuleException("El pedido ya no se puede modificar (estado " + r.getEstado() + ")");
        }
        aplicarItems(r, req.items());
        return toResponse(r);
    }

    @Transactional
    public ReservaResponse cancelar(Long id) {
        Reserva r = propiaDelCliente(id);
        if (!r.getEstado().puedePasarA(EstadoReserva.CANCELADA)) {
            throw new BusinessRuleException("No se puede cancelar una reserva en estado " + r.getEstado());
        }
        r.setEstado(EstadoReserva.CANCELADA);
        return toResponse(r);
    }

    // ===================== RECEPCIONISTA / ADMIN =====================

    @Transactional(readOnly = true)
    public PageResponse<ReservaResponse> listarDelRestaurante(EstadoReserva estado, LocalDate desde, LocalDate hasta,
                                                              String nombreCliente, int page, int size) {
        Specification<Reserva> spec = Specification.allOf(
                ReservaSpecs.delRestaurante(currentUser.restauranteDelStaff().getId()),
                ReservaSpecs.estado(estado), ReservaSpecs.desde(desde), ReservaSpecs.hasta(hasta),
                ReservaSpecs.nombreCliente(nombreCliente));
        return PageResponse.of(repo.findAll(spec, Paging.of(page, size, Sort.by("fechaHora")))
                .map(this::toResponse));
    }

    @Transactional
    public ReservaResponse cambiarEstado(Long id, CambiarEstadoRequest req) {
        Long rid = currentUser.restauranteDelStaff().getId();
        Reserva r = repo.findById(id).filter(x -> x.getRestaurante().getId().equals(rid))
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (!r.getEstado().puedePasarA(req.estado())) {
            throw new BusinessRuleException("Transición no permitida: " + r.getEstado() + " -> " + req.estado());
        }
        if (req.estado() == EstadoReserva.CONFIRMADA) {
            asignarMesa(r, req.mesaId());
        }
        r.setEstado(req.estado());
        return toResponse(r);
    }

    // ===================== helpers =====================

    private void asignarMesa(Reserva r, Long mesaIdSolicitada) {
        Long mesaId = mesaIdSolicitada != null ? mesaIdSolicitada : (r.getMesa() != null ? r.getMesa().getId() : null);
        if (mesaId == null) throw new BusinessRuleException("Debes indicar una mesa para confirmar la reserva");

        Mesa mesa = mesaRepo.findByIdAndRestauranteId(mesaId, r.getRestaurante().getId())
                .filter(Mesa::isActiva)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));
        if (mesa.getCapacidad() < r.getNumPersonas()) {
            throw new BusinessRuleException("La mesa " + mesa.getNumero() + " no tiene capacidad suficiente");
        }
        int dur = r.getRestaurante().getDuracionReservaMin();
        boolean ocupada = repo.existeSolapamiento(mesa.getId(), r.getId(), ESTADOS_QUE_OCUPAN_MESA,
                r.getFechaHora().minusMinutes(dur), r.getFechaHora().plusMinutes(dur));
        if (ocupada) throw new BusinessRuleException("La mesa " + mesa.getNumero() + " ya está reservada en ese horario");
        r.setMesa(mesa);
    }

    private void aplicarItems(Reserva r, List<ItemRequest> items) {
        r.getItems().clear();
        if (items == null) return;
        for (ItemRequest i : items) {
            Producto p = productoRepo.findByIdAndRestauranteIdAndActivoTrue(i.productoId(), r.getRestaurante().getId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "El producto " + i.productoId() + " no pertenece a este restaurante"));
            if (!p.isDisponible()) {
                throw new BusinessRuleException("El producto '" + p.getNombre() + "' no está disponible");
            }
            r.getItems().add(ReservaItem.builder().reserva(r).producto(p).cantidad(i.cantidad())
                    .precioUnitario(p.getPrecio()).nota(i.nota()).build());
        }
    }

    private Reserva propiaDelCliente(Long id) {
        Long uid = currentUser.get().getId();
        return repo.findById(id).filter(r -> r.getCliente().getId().equals(uid))
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
    }

    /** Acceso: el cliente dueño o el staff del mismo restaurante. Si no, 404 (no filtra existencia). */
    private Reserva cargarConAcceso(Long id) {
        User u = currentUser.get();
        Reserva r = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        boolean esDueno = r.getCliente().getId().equals(u.getId());
        boolean esStaff = u.getRole() != Role.CLIENTE && u.getRestaurante() != null
                && u.getRestaurante().getId().equals(r.getRestaurante().getId());
        if (!esDueno && !esStaff) throw new ResourceNotFoundException("Reserva no encontrada");
        return r;
    }

    private ReservaResponse toResponse(Reserva r) {
        List<ItemResponse> items = r.getItems().stream().map(i -> new ItemResponse(
                i.getProducto().getId(), i.getProducto().getNombre(), i.getCantidad(), i.getPrecioUnitario(),
                i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())), i.getNota())).toList();
        BigDecimal total = items.stream().map(ItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Mesa m = r.getMesa();
        return new ReservaResponse(r.getId(), r.getRestaurante().getId(), r.getRestaurante().getNombre(),
                r.getCliente().getId(), r.getCliente().getNombre(),
                m == null ? null : m.getId(), m == null ? null : m.getNumero(),
                r.getFechaHora(), r.getNumPersonas(), r.getEstado(), r.getNotas(), items, total);
    }
}
