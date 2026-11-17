package com.quickorder.restaurant.core.mesa.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMesaRepository extends JpaRepository<MesaEntity, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from MesaEntity m where m.id = :id")
  java.util.Optional<MesaEntity> findByIdForUpdate(@Param("id") UUID id);

  boolean existsByNumero(String numero);

  boolean existsByNumeroAndIdNot(String numero, UUID id);
}
