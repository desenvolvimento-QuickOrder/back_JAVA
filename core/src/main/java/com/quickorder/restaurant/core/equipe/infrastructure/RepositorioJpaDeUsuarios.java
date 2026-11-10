package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.Papel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioJpaDeUsuarios extends JpaRepository<UsuarioJpaEntity, UUID> {

  Optional<UsuarioJpaEntity> findByLoginAndPapelAndAtivoTrue(String login, Papel papel);

  boolean existsByPapelAndAtivoTrue(Papel papel);
}
