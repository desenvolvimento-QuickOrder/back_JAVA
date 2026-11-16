package com.quickorder.restaurant.core.equipe.infrastructure;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioJpaDeSessoesDeRefresh
    extends JpaRepository<SessaoDeRefreshJpaEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select sessao from SessaoDeRefreshJpaEntity sessao where sessao.tokenHash = :hash")
  Optional<SessaoDeRefreshJpaEntity> buscarPorHashBloqueando(@Param("hash") String hash);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update SessaoDeRefreshJpaEntity sessao set sessao.revogadoEm = :instante "
          + "where sessao.familiaId = :familiaId and sessao.revogadoEm is null")
  int revogarFamilia(@Param("familiaId") UUID familiaId, @Param("instante") Instant instante);
}
