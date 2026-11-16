package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.GeradorDeTokenOpaco;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EncerrarSessao {

  private final RepositorioDeSessoesDeRefresh sessoes;
  private final GeradorDeTokenOpaco geradorDeToken;
  private final Relogio relogio;

  public EncerrarSessao(
      RepositorioDeSessoesDeRefresh sessoes, GeradorDeTokenOpaco geradorDeToken, Relogio relogio) {
    this.sessoes = sessoes;
    this.geradorDeToken = geradorDeToken;
    this.relogio = relogio;
  }

  @Transactional
  public void executar(String refreshToken) {
    sessoes
        .buscarPorHashBloqueando(geradorDeToken.hash(refreshToken))
        .ifPresent(sessao -> sessoes.revogarFamilia(sessao.familiaId(), relogio.agora()));
  }
}
