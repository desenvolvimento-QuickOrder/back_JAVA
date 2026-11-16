package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.ConflitoDeNegocio;
import com.quickorder.restaurant.core.shared.error.ErroDeCampo;
import com.quickorder.restaurant.core.shared.error.ErroDeValidacaoDaRequisicao;
import com.quickorder.restaurant.core.shared.tenant.TenantContext;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CriarUsuario {

  private static final Pattern LOGIN_VALIDO = Pattern.compile("^[a-z0-9._-]{3,80}$");

  private final RepositorioDeUsuarios usuarios;
  private final HashDeSenha hashDeSenha;
  private final TenantContext tenantContext;

  public CriarUsuario(
      RepositorioDeUsuarios usuarios, HashDeSenha hashDeSenha, TenantContext tenantContext) {
    this.usuarios = usuarios;
    this.hashDeSenha = hashDeSenha;
    this.tenantContext = tenantContext;
  }

  @Transactional
  public Usuario executar(String nome, Papel papel, String login, String senha) {
    if (nome == null || nome.isBlank() || nome.length() > 120) {
      throw erro("nome", "Nome obrigatório, com até 120 caracteres.");
    }
    if (papel != Papel.ADMIN) {
      throw erro("papel", "Na criação, o papel deve ser ADMIN.");
    }
    if (login == null || !LOGIN_VALIDO.matcher(login.trim().toLowerCase(Locale.ROOT)).matches()) {
      throw erro("login", "Login obrigatório, com 3 a 80 caracteres válidos.");
    }
    if (senha == null || senha.length() < 8 || senha.length() > 128) {
      throw erro("senha", "Senha obrigatória, com 8 a 128 caracteres.");
    }
    String loginNormalizado = login.trim().toLowerCase(Locale.ROOT);
    if (usuarios.existeLogin(loginNormalizado)) {
      throw new ConflitoDeNegocio("LOGIN_JA_EXISTE", "O login já existe neste restaurante.");
    }
    UUID restauranteId =
        tenantContext
            .tenantAtual()
            .orElseThrow(() -> new IllegalStateException("Tenant ausente no contexto."));
    return usuarios.salvar(
        Usuario.novoAdmin(
            UUID.randomUUID(), restauranteId, nome, loginNormalizado, hashDeSenha.gerar(senha)));
  }

  private static ErroDeValidacaoDaRequisicao erro(String campo, String mensagem) {
    return new ErroDeValidacaoDaRequisicao(List.of(new ErroDeCampo(campo, mensagem)));
  }
}
