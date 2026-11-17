package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.Papel;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioJpaDeUsuarios extends JpaRepository<UsuarioJpaEntity, UUID> {

  Optional<UsuarioJpaEntity> findByLoginAndPapelAndAtivoTrue(String login, Papel papel);

  Optional<UsuarioJpaEntity> findByCodigo(String codigo);

  boolean existsByPapelAndAtivoTrue(Papel papel);

  boolean existsByLogin(String login);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select usuario from UsuarioJpaEntity usuario "
          + "where usuario.papel = com.quickorder.restaurant.core.equipe.domain.Papel.ADMIN "
          + "and usuario.ativo = true order by usuario.id")
  List<UsuarioJpaEntity> bloquearAdminsAtivos();

  @Query(
      "select usuario from UsuarioJpaEntity usuario "
          + "where (:papel is null or usuario.papel = :papel) "
          + "and (:ativo is null or usuario.ativo = :ativo)")
  List<UsuarioJpaEntity> listar(
      @Param("papel") Papel papel, @Param("ativo") Boolean ativo, Pageable pageable);

  @Query(
      "select count(usuario) from UsuarioJpaEntity usuario "
          + "where (:papel is null or usuario.papel = :papel) "
          + "and (:ativo is null or usuario.ativo = :ativo)")
  long contar(@Param("papel") Papel papel, @Param("ativo") Boolean ativo);
}
