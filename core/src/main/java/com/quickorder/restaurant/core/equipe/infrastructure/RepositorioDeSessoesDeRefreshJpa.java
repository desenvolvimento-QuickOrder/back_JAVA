package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.SessaoDeRefresh;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeSessoesDeRefreshJpa implements RepositorioDeSessoesDeRefresh {

  private final RepositorioJpaDeSessoesDeRefresh repositorio;

  RepositorioDeSessoesDeRefreshJpa(RepositorioJpaDeSessoesDeRefresh repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public Optional<SessaoDeRefresh> buscarPorHashBloqueando(String tokenHash) {
    return repositorio
        .buscarPorHashBloqueando(tokenHash)
        .map(SessaoDeRefreshJpaEntity::paraDominio);
  }

  @Override
  public void salvar(SessaoDeRefresh sessao) {
    repositorio.saveAndFlush(new SessaoDeRefreshJpaEntity(sessao));
  }

  @Override
  public void revogarFamilia(UUID familiaId, Instant instante) {
    repositorio.revogarFamilia(familiaId, instante);
  }
}
