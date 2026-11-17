package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeRestaurantes;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Restaurante;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.equipe.infrastructure.BootstrapDoAdmin;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.transaction.PlatformTransactionManager;

class BootstrapDoAdminTest {

  @Test
  void deveCriarAdminNoTenantExistenteSomenteQuandoNaoHaAdminAtivo() {
    UUID restauranteId = UUID.randomUUID();
    var restaurantes = mock(RepositorioDeRestaurantes.class);
    var usuarios = mock(RepositorioDeUsuarios.class);
    var hash = mock(HashDeSenha.class);
    when(restaurantes.buscarPorSlug("restaurante-dev"))
        .thenReturn(Optional.of(new Restaurante(restauranteId, "restaurante-dev", true)));
    when(usuarios.possuiAdminAtivo()).thenReturn(false);
    when(hash.gerar("senha-segura")).thenReturn("hash-cifrado");
    var execucao = new ExecucaoImediata();

    new BootstrapDoAdmin(
            "restaurante-dev", " Admin ", "senha-segura", restaurantes, usuarios, hash, execucao)
        .run(new DefaultApplicationArguments());

    assertThat(execucao.tenantId).isEqualTo(restauranteId);
    var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
    verify(usuarios).salvar(captor.capture());
    assertThat(captor.getValue().papel()).isEqualTo(Papel.ADMIN);
    assertThat(captor.getValue().login()).isEqualTo("admin");
    assertThat(captor.getValue().ativo()).isTrue();
    assertThat(captor.getValue().senhaHash()).isEqualTo("hash-cifrado");
  }

  @Test
  void naoDeveCriarAdminNemRestauranteQuandoConfiguracaoEstaIncompletaOuAdminJaExiste() {
    var restaurantes = mock(RepositorioDeRestaurantes.class);
    var usuarios = mock(RepositorioDeUsuarios.class);
    var hash = mock(HashDeSenha.class);
    var execucao = new ExecucaoImediata();
    new BootstrapDoAdmin(
            "restaurante-dev", "", "senha-segura", restaurantes, usuarios, hash, execucao)
        .run(new DefaultApplicationArguments());
    verify(restaurantes, never()).buscarPorSlug(any());
    verify(usuarios, never()).salvar(any());

    when(restaurantes.buscarPorSlug("restaurante-dev"))
        .thenReturn(Optional.of(new Restaurante(UUID.randomUUID(), "restaurante-dev", true)));
    when(usuarios.possuiAdminAtivo()).thenReturn(true);
    new BootstrapDoAdmin(
            "restaurante-dev", "admin", "senha-segura", restaurantes, usuarios, hash, execucao)
        .run(new DefaultApplicationArguments());
    verify(usuarios, never()).salvar(any());
  }

  private static class ExecucaoImediata extends ExecucaoNoTenant {

    private UUID tenantId;

    ExecucaoImediata() {
      super(mock(PlatformTransactionManager.class));
    }

    @Override
    public <T> T executar(UUID tenantId, Supplier<T> operacao) {
      this.tenantId = tenantId;
      return operacao.get();
    }
  }
}
