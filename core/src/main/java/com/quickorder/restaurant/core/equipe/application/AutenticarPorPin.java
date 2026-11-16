package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.CredenciaisInvalidas;
import com.quickorder.restaurant.core.equipe.domain.Dispositivo;
import com.quickorder.restaurant.core.equipe.domain.GeradorDeTokenOpaco;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativas;
import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativasExcedido;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeDispositivos;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeRestaurantes;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Restaurante;
import com.quickorder.restaurant.core.equipe.domain.ResultadoDeSessao;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AutenticarPorPin {

  private final RepositorioDeRestaurantes restaurantes;
  private final RepositorioDeUsuarios usuarios;
  private final RepositorioDeDispositivos dispositivos;
  private final ExecucaoNoTenant execucaoNoTenant;
  private final HashDeSenha hashDeSenha;
  private final GeradorDeTokenOpaco geradorDeToken;
  private final LimiteDeTentativas limiteDeTentativas;
  private final CriadorDeSessao criadorDeSessao;
  private final Relogio relogio;
  private final String hashFicticio;

  public AutenticarPorPin(
      RepositorioDeRestaurantes restaurantes,
      RepositorioDeUsuarios usuarios,
      RepositorioDeDispositivos dispositivos,
      ExecucaoNoTenant execucaoNoTenant,
      HashDeSenha hashDeSenha,
      GeradorDeTokenOpaco geradorDeToken,
      LimiteDeTentativas limiteDeTentativas,
      CriadorDeSessao criadorDeSessao,
      Relogio relogio) {
    this.restaurantes = restaurantes;
    this.usuarios = usuarios;
    this.dispositivos = dispositivos;
    this.execucaoNoTenant = execucaoNoTenant;
    this.hashDeSenha = hashDeSenha;
    this.geradorDeToken = geradorDeToken;
    this.limiteDeTentativas = limiteDeTentativas;
    this.criadorDeSessao = criadorDeSessao;
    this.relogio = relogio;
    hashFicticio = hashDeSenha.gerar(UUID.randomUUID().toString());
  }

  public ResultadoDeSessao executar(
      String slug, String codigo, String pin, String tokenDoDispositivo, String ipDoCliente) {
    Instant agora = relogio.agora();
    long esperaPorIp = limiteDeTentativas.reservarTentativaPorIp(ipDoCliente, agora);
    if (esperaPorIp > 0) {
      throw new LimiteDeTentativasExcedido(esperaPorIp);
    }

    String tokenHash =
        tokenDoDispositivo == null || tokenDoDispositivo.isBlank()
            ? "ausente"
            : geradorDeToken.hash(tokenDoDispositivo);
    String conta = "pin:" + normalizar(slug) + ":" + codigo;
    long esperaDaConta = limiteDeTentativas.tempoRestanteDeBloqueioDaConta(conta, agora);
    long esperaDoDispositivo =
        limiteDeTentativas.tempoRestanteDeBloqueioDoDispositivo(tokenHash, agora);
    if (esperaDaConta > 0 || esperaDoDispositivo > 0) {
      throw new LimiteDeTentativasExcedido(Math.max(esperaDaConta, esperaDoDispositivo));
    }

    Restaurante restaurante = restaurantes.buscarPorSlug(normalizar(slug)).orElse(null);
    if (restaurante == null || !restaurante.ativo()) {
      falhar(pin, conta, tokenHash, agora);
      throw new CredenciaisInvalidas();
    }

    Credenciais credenciais =
        execucaoNoTenant.executar(
            restaurante.id(),
            () ->
                new Credenciais(
                    tokenDoDispositivo == null || tokenDoDispositivo.isBlank()
                        ? null
                        : dispositivos.buscarPorHashDoToken(tokenHash).orElse(null),
                    usuarios.buscarPorCodigo(codigo).orElse(null)));
    Usuario usuario = credenciais.usuario();
    Dispositivo dispositivo = credenciais.dispositivo();
    boolean dispositivoValido = dispositivo != null && dispositivo.ativo();
    boolean usuarioValido =
        usuario != null
            && usuario.ativo()
            && (usuario.papel() == Papel.GARCOM || usuario.papel() == Papel.COPA);
    if (!dispositivoValido || !usuarioValido) {
      hashDeSenha.corresponde(pin, hashFicticio);
      falhar(conta, tokenHash, agora);
      throw new CredenciaisInvalidas();
    }
    if (!hashDeSenha.corresponde(pin, usuario.pinHash())) {
      falhar(conta, tokenHash, agora);
      throw new CredenciaisInvalidas();
    }

    execucaoNoTenant.executar(
        restaurante.id(),
        () -> {
          dispositivos.salvar(dispositivo.marcarUso(agora));
          return null;
        });
    ResultadoDeSessao sessao = criadorDeSessao.criarNovaFamilia(usuario, agora, dispositivo.id());
    limiteDeTentativas.registrarSucessoDaConta(conta);
    limiteDeTentativas.registrarSucessoDoDispositivo(tokenHash);
    return sessao;
  }

  private void falhar(String pin, String conta, String tokenHash, Instant instante) {
    hashDeSenha.corresponde(pin, hashFicticio);
    falhar(conta, tokenHash, instante);
  }

  private void falhar(String conta, String tokenHash, Instant instante) {
    limiteDeTentativas.registrarFalhaDaConta(conta, instante);
    limiteDeTentativas.registrarFalhaDoDispositivo(tokenHash, instante);
  }

  private static String normalizar(String valor) {
    return valor.trim().toLowerCase(Locale.ROOT);
  }

  private record Credenciais(Dispositivo dispositivo, Usuario usuario) {}
}
