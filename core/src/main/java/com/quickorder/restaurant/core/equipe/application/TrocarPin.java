package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionario;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Pin;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.SessaoInvalida;
import com.quickorder.restaurant.core.shared.error.ErroDeCampo;
import com.quickorder.restaurant.core.shared.error.ErroDeValidacaoDaRequisicao;
import com.quickorder.restaurant.core.shared.error.ExcecaoDeNegocio;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TrocarPin {

  private final RepositorioDeUsuarios usuarios;
  private final RepositorioDeSessoesDeRefresh sessoes;
  private final HashDeSenha hashDeSenha;
  private final ExecucaoNoTenant execucaoNoTenant;
  private final Relogio relogio;

  public TrocarPin(
      RepositorioDeUsuarios usuarios,
      RepositorioDeSessoesDeRefresh sessoes,
      HashDeSenha hashDeSenha,
      ExecucaoNoTenant execucaoNoTenant,
      Relogio relogio) {
    this.usuarios = usuarios;
    this.sessoes = sessoes;
    this.hashDeSenha = hashDeSenha;
    this.execucaoNoTenant = execucaoNoTenant;
    this.relogio = relogio;
  }

  public void executar(UUID restauranteId, UUID usuarioId, String pinAtual, String pinNovo) {
    execucaoNoTenant.executar(
        restauranteId,
        () -> {
          var usuario = usuarios.buscarPorId(usuarioId).orElseThrow(SessaoInvalida::new);
          if (!usuario.ativo()
              || (usuario.papel() != Papel.GARCOM && usuario.papel() != Papel.COPA)) {
            throw new SessaoInvalida();
          }
          if (!hashDeSenha.corresponde(pinAtual, usuario.pinHash())) {
            throw new ExcecaoDeNegocio("PIN_ATUAL_INVALIDO", "PIN atual inválido.");
          }
          Pin novo;
          try {
            novo = new Pin(pinNovo);
          } catch (IllegalArgumentException exception) {
            throw new ErroDeValidacaoDaRequisicao(
                List.of(new ErroDeCampo("pinNovo", exception.getMessage())));
          }
          if (!novo.podeSerUsadoCom(new CodigoFuncionario(usuario.codigo()))) {
            throw new ErroDeValidacaoDaRequisicao(
                List.of(new ErroDeCampo("pinNovo", "PIN não atende às regras de segurança.")));
          }
          usuarios.salvar(usuario.trocarPin(hashDeSenha.gerar(pinNovo)));
          sessoes.revogarTodasDoUsuario(usuario.id(), relogio.agora());
          return null;
        });
  }
}
