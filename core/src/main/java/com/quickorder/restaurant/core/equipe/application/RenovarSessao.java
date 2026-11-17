package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.GeradorDeTokenOpaco;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeDispositivos;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeRestaurantes;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Restaurante;
import com.quickorder.restaurant.core.equipe.domain.ResultadoDeSessao;
import com.quickorder.restaurant.core.equipe.domain.SessaoInvalida;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenovarSessao {

  private static final Duration JANELA_DE_REUSO = Duration.ofSeconds(30);

  private final RepositorioDeSessoesDeRefresh sessoes;
  private final RepositorioDeRestaurantes restaurantes;
  private final RepositorioDeUsuarios usuarios;
  private final RepositorioDeDispositivos dispositivos;
  private final ExecucaoNoTenant execucaoNoTenant;
  private final CriadorDeSessao criadorDeSessao;
  private final GeradorDeTokenOpaco geradorDeToken;
  private final Relogio relogio;

  public RenovarSessao(
      RepositorioDeSessoesDeRefresh sessoes,
      RepositorioDeRestaurantes restaurantes,
      RepositorioDeUsuarios usuarios,
      RepositorioDeDispositivos dispositivos,
      ExecucaoNoTenant execucaoNoTenant,
      CriadorDeSessao criadorDeSessao,
      GeradorDeTokenOpaco geradorDeToken,
      Relogio relogio) {
    this.sessoes = sessoes;
    this.restaurantes = restaurantes;
    this.usuarios = usuarios;
    this.dispositivos = dispositivos;
    this.execucaoNoTenant = execucaoNoTenant;
    this.criadorDeSessao = criadorDeSessao;
    this.geradorDeToken = geradorDeToken;
    this.relogio = relogio;
  }

  @Transactional(noRollbackFor = SessaoInvalida.class)
  public ResultadoDeSessao executar(String refreshToken) {
    return executar(refreshToken, null);
  }

  @Transactional(noRollbackFor = SessaoInvalida.class)
  public ResultadoDeSessao executar(String refreshToken, String tokenDoDispositivo) {
    Instant agora = relogio.agora();
    var sessao = sessoes.buscarPorHashBloqueando(geradorDeToken.hash(refreshToken));
    if (sessao.isEmpty()) {
      throw new SessaoInvalida();
    }

    var atual = sessao.get();
    if (atual.dispositivoId() != null) {
      if (tokenDoDispositivo == null || tokenDoDispositivo.isBlank()) {
        throw new SessaoInvalida();
      }
      String hashDoDispositivo = geradorDeToken.hash(tokenDoDispositivo);
      var dispositivo =
          execucaoNoTenant.executar(
              atual.restauranteId(),
              () -> dispositivos.buscarPorId(atual.dispositivoId()).orElse(null));
      if (dispositivo == null
          || !dispositivo.ativo()
          || !dispositivo.tokenHash().equals(hashDoDispositivo)) {
        throw new SessaoInvalida();
      }
    }
    if (atual.revogadoEm() != null) {
      sessoes.revogarFamilia(atual.familiaId(), agora);
      throw new SessaoInvalida();
    }
    if (!atual.expiraEm().isAfter(agora) || !atual.familiaExpiraEm().isAfter(agora)) {
      throw new SessaoInvalida();
    }

    boolean dentroDaJanelaDeReuso =
        atual.usadoEm() != null && !agora.isAfter(atual.usadoEm().plus(JANELA_DE_REUSO));
    if (atual.usadoEm() != null && !dentroDaJanelaDeReuso) {
      sessoes.revogarFamilia(atual.familiaId(), agora);
      throw new SessaoInvalida();
    }

    Optional<Restaurante> restaurante = restaurantes.buscarPorId(atual.restauranteId());
    if (restaurante.isEmpty() || !restaurante.get().ativo()) {
      throw new SessaoInvalida();
    }

    Usuario usuario =
        execucaoNoTenant.executar(
            atual.restauranteId(), () -> usuarios.buscarPorId(atual.usuarioId()).orElse(null));
    if (usuario == null || !usuario.ativo()) {
      throw new SessaoInvalida();
    }

    if (atual.usadoEm() == null) {
      sessoes.salvar(atual.marcarComoUsada(agora));
    }
    return criadorDeSessao.criarNaFamilia(
        usuario, atual.familiaId(), atual.familiaExpiraEm(), agora, atual.dispositivoId());
  }
}
