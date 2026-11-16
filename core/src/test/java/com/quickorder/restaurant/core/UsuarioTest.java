package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioTest {

  @Test
  void fabricaAdminNormalizaLoginEIniciaAtivo() {
    UUID id = UUID.randomUUID();
    UUID restauranteId = UUID.randomUUID();

    Usuario usuario = Usuario.novoAdmin(id, restauranteId, "Ana", " ANA.ADMIN ", "hash");

    assertThat(usuario.id()).isEqualTo(id);
    assertThat(usuario.restauranteId()).isEqualTo(restauranteId);
    assertThat(usuario.login()).isEqualTo("ana.admin");
    assertThat(usuario.papel()).isEqualTo(Papel.ADMIN);
    assertThat(usuario.ativo()).isTrue();
    assertThat(usuario.versao()).isZero();
  }

  @Test
  void renomearTrocarSenhaETrocarPapelPreservamIdentidade() {
    Usuario admin =
        Usuario.novoAdmin(UUID.randomUUID(), UUID.randomUUID(), "Ana", "ana.admin", "hash-antigo");

    Usuario atualizado =
        admin.renomear("Ana Maria").trocarSenha("hash-novo").trocarPapel(Papel.ADMIN);

    assertThat(atualizado.nome()).isEqualTo("Ana Maria");
    assertThat(atualizado.senhaHash()).isEqualTo("hash-novo");
    assertThat(atualizado.papel()).isEqualTo(Papel.ADMIN);
    assertThat(atualizado.id()).isEqualTo(admin.id());
  }

  @Test
  void desativarEAtivarSaoIdempotentes() {
    Usuario usuario =
        Usuario.novoAdmin(UUID.randomUUID(), UUID.randomUUID(), "Ana", "ana.admin", "hash");

    Usuario desativado = usuario.desativar();

    assertThat(desativado.ativo()).isFalse();
    assertThat(desativado.desativar()).isSameAs(desativado);
    Usuario ativado = desativado.ativar();
    assertThat(ativado.ativo()).isTrue();
    assertThat(ativado.ativar()).isSameAs(ativado);
  }

  @Test
  void trocaDePapelSoPermiteAlternarGarcomECopa() {
    Usuario garcom =
        new Usuario(UUID.randomUUID(), UUID.randomUUID(), "João", null, null, Papel.GARCOM, true);

    assertThat(garcom.trocarPapel(Papel.COPA).papel()).isEqualTo(Papel.COPA);
    assertThat(garcom.trocarPapel(Papel.GARCOM)).isSameAs(garcom);
    assertThatThrownBy(() -> garcom.trocarPapel(Papel.ADMIN))
        .isInstanceOf(com.quickorder.restaurant.core.equipe.domain.TrocaDePapelNaoPermitida.class);
  }
}
