package com.tablego.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tablego.api.model.EstadoReserva;
import com.tablego.api.model.Reserva;

import java.time.LocalDateTime;
import java.util.Collection;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "restaurante", "mesa"})
    Page<Reserva> findAll(Specification<Reserva> spec, Pageable pageable);

    /** Reservas activas de la mesa que se solapan con la ventana (desde, hasta) exclusiva. */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.mesa.id = :mesaId and r.id <> :reservaId and r.estado in :estados
              and r.fechaHora > :desde and r.fechaHora < :hasta
            """)
    boolean existeSolapamiento(@Param("mesaId") Long mesaId, @Param("reservaId") Long reservaId,
                               @Param("estados") Collection<EstadoReserva> estados,
                               @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
