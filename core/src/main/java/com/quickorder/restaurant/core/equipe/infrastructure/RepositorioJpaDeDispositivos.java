package com.quickorder.restaurant.core.equipe.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepositorioJpaDeDispositivos extends JpaRepository<DispositivoJpaEntity, UUID> {

  Optional<DispositivoJpaEntity> findByTokenHash(String tokenHash);

  @Query("select d from DispositivoJpaEntity d order by d.criadoEm desc, d.id")
  List<DispositivoJpaEntity> listar(Pageable pageable);

  @Query("select count(d) from DispositivoJpaEntity d")
  long contar();
}
