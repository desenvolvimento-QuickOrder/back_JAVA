package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.CredenciaisInvalidas;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativas;
import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativasExcedido;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeRestaurantes;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Restaurante;
import com.quickorder.restaurant.core.equipe.domain.ResultadoDeSessao;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class Autenticar {

  private final RepositorioDeRestaurantes restaurantes;
  private final RepositorioDeUsuarios usuarios;
  private final ExecucaoNoTenant execucaoNoTenant;
  private final HashDeSenha hashDeSenha;
  private final LimiteDeTentativas limiteDeTentativas;
  private final CriadorDeSessao criadorDeSessao;
  private final Relogio relogio;
  private final String hashFicticio;

  public Autenticar(
      RepositorioDeRestaurantes restaurantes,
      RepositorioDeUsuarios usuarios,
      ExecucaoNoTenant execucaoNoTenant,
      HashDeSenha hashDeSenha,
      LimiteDeTentativas limiteDeTentativas,
      CriadorDeSessao criadorDeSessao,
      Relogio relogio) {
    this.restaurantes = restaurantes;
    this.usuarios = usuarios;
    this.execucaoNoTenant = execucaoNoTenant;
    this.hashDeSenha = hashDeSenha;
    this.limiteDeTentativas = limiteDeTentativas;
    this.criadorDeSessao = criadorDeSessao;
    this.relogio = relogio;
    hashFicticio = hashDeSenha.gerar(UUID.randomUUID().toString());
  }

  public ResultadoDeSessao executar(String slug, String login, String senha, String ipDoCliente) {
    var agora = relogio.agora();
    long esperaPorIp = limiteDeTentativas.reservarTentativaPorIp(ipDoCliente, agora);
    if (esperaPorIp > 0) {
      throw new LimiteDeTentativasExcedido(esperaPorIp);
    }

    String conta = normalizar(slug) + ":" + normalizar(login);
    long esperaDaConta = limiteDeTentativas.tempoRestanteDeBloqueioDaConta(conta, agora);
    if (esperaDaConta > 0) {
      throw new LimiteDeTentativasExcedido(esperaDaConta);
    }

    Restaurante restaurante = restaurantes.buscarPorSlug(normalizar(slug)).orElse(null);
    if (restaurante == null || !restaurante.ativo()) {
      falhar(senha, conta, agora);
      throw new CredenciaisInvalidas();
    }

    Usuario usuario =
        execucaoNoTenant.executar(
            restaurante.id(), () -> usuarios.buscarPorLogin(normalizar(login)).orElse(null));
    if (usuario == null || !usuario.ativo() || usuario.papel() != Papel.ADMIN) {
      falhar(senha, conta, agora);
      throw new CredenciaisInvalidas();
    }

    if (!hashDeSenha.corresponde(senha, usuario.senhaHash())) {
      limiteDeTentativas.registrarFalhaDaConta(conta, agora);
      throw new CredenciaisInvalidas();
    }

    ResultadoDeSessao sessao = criadorDeSessao.criarNovaFamilia(usuario, agora);
    limiteDeTentativas.registrarSucessoDaConta(conta);
    return sessao;
  }

  private void falhar(String senha, String conta, java.time.Instant instante) {
    hashDeSenha.corresponde(senha, hashFicticio);
    limiteDeTentativas.registrarFalhaDaConta(conta, instante);
  }

  private static String normalizar(String valor) {
    return valor.trim().toLowerCase(Locale.ROOT);
  }
}
