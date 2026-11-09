package com.tablego.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.tablego.api.model.Mesa;

import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long>, JpaSpecificationExecutor<Mesa> {
    Optional<Mesa> findByIdAndRestauranteId(Long id, Long restauranteId);
    boolean existsByRestauranteIdAndNumero(Long restauranteId, Integer numero);
    boolean existsByRestauranteIdAndActivaTrueAndCapacidadGreaterThanEqual(Long restauranteId, Integer capacidad);
}
