package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.EmissorDeJwt;
import com.quickorder.restaurant.core.equipe.domain.GeradorDeTokenOpaco;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.ResultadoDeSessao;
import com.quickorder.restaurant.core.equipe.domain.SessaoDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CriadorDeSessao {

  public static final int DURACAO_ACCESS_TOKEN_SEGUNDOS = 15 * 60;
  public static final Duration DURACAO_DA_FAMILIA = Duration.ofHours(12);

  private final EmissorDeJwt emissorDeJwt;
  private final GeradorDeTokenOpaco geradorDeToken;
  private final RepositorioDeSessoesDeRefresh repositorio;
  private final Relogio relogio;

  public CriadorDeSessao(
      EmissorDeJwt emissorDeJwt,
      GeradorDeTokenOpaco geradorDeToken,
      RepositorioDeSessoesDeRefresh repositorio,
      Relogio relogio) {
    this.emissorDeJwt = emissorDeJwt;
    this.geradorDeToken = geradorDeToken;
    this.repositorio = repositorio;
    this.relogio = relogio;
  }

  public ResultadoDeSessao criarNovaFamilia(Usuario usuario, Instant instante) {
    return criar(usuario, UUID.randomUUID(), instante.plus(DURACAO_DA_FAMILIA), instante);
  }

  public ResultadoDeSessao criarNaFamilia(
      Usuario usuario, UUID familiaId, Instant familiaExpiraEm, Instant instante) {
    return criar(usuario, familiaId, familiaExpiraEm, instante);
  }

  private ResultadoDeSessao criar(
      Usuario usuario, UUID familiaId, Instant familiaExpiraEm, Instant instante) {
    String accessToken = emissorDeJwt.emitir(usuario, instante);
    String refreshToken = geradorDeToken.gerar();
    Instant agora = relogio.agora();
    repositorio.salvar(
        new SessaoDeRefresh(
            UUID.randomUUID(),
            usuario.restauranteId(),
            usuario.id(),
            geradorDeToken.hash(refreshToken),
            familiaId,
            agora,
            familiaExpiraEm,
            familiaExpiraEm,
            null,
            null));
    return new ResultadoDeSessao(accessToken, refreshToken, DURACAO_ACCESS_TOKEN_SEGUNDOS);
  }
}
