package com.quickorder.restaurant.core.equipe.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeSessoesDeRefresh {

  Optional<SessaoDeRefresh> buscarPorHashBloqueando(String tokenHash);

  void salvar(SessaoDeRefresh sessao);

  void revogarFamilia(UUID familiaId, Instant instante);
}
