package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionario;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Pin;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.TrocaDePapelNaoPermitida;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.ErroDeCampo;
import com.quickorder.restaurant.core.shared.error.ErroDeValidacaoDaRequisicao;
import com.quickorder.restaurant.core.shared.error.ExcecaoDeNegocio;
import com.quickorder.restaurant.core.shared.error.RecursoNaoEncontrado;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtualizarUsuario {

  private final RepositorioDeUsuarios usuarios;
  private final RepositorioDeSessoesDeRefresh sessoes;
  private final HashDeSenha hashDeSenha;
  private final Relogio relogio;

  public AtualizarUsuario(
      RepositorioDeUsuarios usuarios,
      RepositorioDeSessoesDeRefresh sessoes,
      HashDeSenha hashDeSenha,
      Relogio relogio) {
    this.usuarios = usuarios;
    this.sessoes = sessoes;
    this.hashDeSenha = hashDeSenha;
    this.relogio = relogio;
  }

  @Transactional
  public Usuario executar(
      UUID id,
      UUID idDoSolicitante,
      CampoAtualizacao<String> nome,
      CampoAtualizacao<Papel> papel,
      CampoAtualizacao<String> senha,
      CampoAtualizacao<String> senhaAtual,
      CampoAtualizacao<String> pin) {
    List<ErroDeCampo> erros = validarCampos(nome, papel, senha, senhaAtual, pin);
    if (!erros.isEmpty()) {
      throw new ErroDeValidacaoDaRequisicao(erros);
    }
    if (!nome.informado() && !papel.informado() && !senha.informado() && !pin.informado()) {
      throw erro("body", "Informe ao menos um campo para atualização.");
    }

    Usuario usuario =
        usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontrado("Usuário"));
    erros = validarCredenciais(usuario, senha, senhaAtual, pin);
    if (!erros.isEmpty()) {
      throw new ErroDeValidacaoDaRequisicao(erros);
    }
    boolean senhaAlterada = senha.informado();
    if (senhaAlterada && id.equals(idDoSolicitante)) {
      if (!senhaAtual.informado()
          || !hashDeSenha.corresponde(senhaAtual.valor(), usuario.senhaHash())) {
        throw new ExcecaoDeNegocio(
            "SENHA_ATUAL_INVALIDA", "A senha atual está ausente ou incorreta.");
      }
    }

    Usuario atualizado = usuario;
    if (nome.informado()) {
      atualizado = atualizado.renomear(nome.valor());
    }
    if (papel.informado() && papel.valor() != usuario.papel()) {
      try {
        atualizado = atualizado.trocarPapel(papel.valor());
      } catch (TrocaDePapelNaoPermitida excecao) {
        throw new ExcecaoDeNegocio(
            "TROCA_DE_PAPEL_NAO_PERMITIDA", "A troca de papel solicitada não é permitida.");
      }
    }
    if (senhaAlterada) {
      atualizado = atualizado.trocarSenha(hashDeSenha.gerar(senha.valor()));
    }
    boolean pinAlterado = pin.informado();
    if (pinAlterado) {
      atualizado = atualizado.redefinirPin(hashDeSenha.gerar(pin.valor()));
    }

    Usuario salvo = usuarios.salvar(atualizado);
    if (senhaAlterada || pinAlterado || (papel.informado() && papel.valor() != usuario.papel())) {
      sessoes.revogarTodasDoUsuario(id, relogio.agora());
    }
    return salvo;
  }

  private static List<ErroDeCampo> validarCampos(
      CampoAtualizacao<String> nome,
      CampoAtualizacao<Papel> papel,
      CampoAtualizacao<String> senha,
      CampoAtualizacao<String> senhaAtual,
      CampoAtualizacao<String> pin) {
    List<ErroDeCampo> erros = new ArrayList<>();
    adicionarErroSeInvalido(erros, "nome", nome, valor -> !valor.isBlank());
    adicionarErroSeInvalido(erros, "papel", papel, valor -> true);
    adicionarErroSeInvalido(
        erros, "senha", senha, valor -> valor.length() >= 8 && valor.length() <= 128);
    adicionarErroSeInvalido(erros, "senhaAtual", senhaAtual, valor -> true);
    adicionarErroSeInvalido(
        erros,
        "pin",
        pin,
        valor -> {
          try {
            return new Pin(valor).podeSerUsado();
          } catch (IllegalArgumentException excecao) {
            return false;
          }
        });
    return erros;
  }

  private static List<ErroDeCampo> validarCredenciais(
      Usuario usuario,
      CampoAtualizacao<String> senha,
      CampoAtualizacao<String> senhaAtual,
      CampoAtualizacao<String> pin) {
    List<ErroDeCampo> erros = new ArrayList<>();
    if (usuario.papel() != Papel.ADMIN) {
      if (senha.informado()) {
        erros.add(new ErroDeCampo("senha", "GARCOM e COPA não podem ter senha."));
      }
      if (senhaAtual.informado()) {
        erros.add(new ErroDeCampo("senhaAtual", "GARCOM e COPA não podem ter senha."));
      }
    }
    if (pin.informado()) {
      if (usuario.papel() == Papel.ADMIN) {
        erros.add(new ErroDeCampo("pin", "ADMIN não pode ter PIN."));
      } else if (!new Pin(pin.valor()).podeSerUsadoCom(new CodigoFuncionario(usuario.codigo()))) {
        erros.add(new ErroDeCampo("pin", "O PIN escolhido não pode ser usado."));
      }
    }
    return erros;
  }

  private static <T> void adicionarErroSeInvalido(
      List<ErroDeCampo> erros,
      String campo,
      CampoAtualizacao<T> atualizacao,
      java.util.function.Predicate<T> valido) {
    if (atualizacao.informado()
        && (atualizacao.valor() == null || !valido.test(atualizacao.valor()))) {
      erros.add(new ErroDeCampo(campo, "O campo deve conter um valor válido."));
    }
  }

  private static ErroDeValidacaoDaRequisicao erro(String campo, String mensagem) {
    return new ErroDeValidacaoDaRequisicao(List.of(new ErroDeCampo(campo, mensagem)));
  }
}
