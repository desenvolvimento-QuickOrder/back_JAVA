package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
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
      CampoAtualizacao<String> senhaAtual) {
    List<ErroDeCampo> erros = validarCampos(nome, papel, senha, senhaAtual);
    if (!erros.isEmpty()) {
      throw new ErroDeValidacaoDaRequisicao(erros);
    }
    if (!nome.informado() && !papel.informado() && !senha.informado()) {
      throw erro("body", "Informe ao menos um campo para atualização.");
    }

    Usuario usuario =
        usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontrado("Usuário"));
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

    Usuario salvo = usuarios.salvar(atualizado);
    if (senhaAlterada || (papel.informado() && papel.valor() != usuario.papel())) {
      sessoes.revogarTodasDoUsuario(id, relogio.agora());
    }
    return salvo;
  }

  private static List<ErroDeCampo> validarCampos(
      CampoAtualizacao<String> nome,
      CampoAtualizacao<Papel> papel,
      CampoAtualizacao<String> senha,
      CampoAtualizacao<String> senhaAtual) {
    List<ErroDeCampo> erros = new ArrayList<>();
    adicionarErroSeInvalido(erros, "nome", nome, valor -> !valor.isBlank());
    adicionarErroSeInvalido(erros, "papel", papel, valor -> true);
    adicionarErroSeInvalido(
        erros, "senha", senha, valor -> valor.length() >= 8 && valor.length() <= 128);
    adicionarErroSeInvalido(
        erros, "senhaAtual", senhaAtual, valor -> valor.length() >= 8 && valor.length() <= 128);
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
