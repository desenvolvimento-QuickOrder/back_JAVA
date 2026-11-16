package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.equipe.application.Autenticar;
import com.quickorder.restaurant.core.equipe.application.CriadorDeSessao;
import com.quickorder.restaurant.core.equipe.domain.CredenciaisInvalidas;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativas;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeRestaurantes;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AutenticarTest {

  @Test
  void deveCompararSenhaFicticiaQuandoOSlugNaoExiste() {
    var restaurantes = mock(RepositorioDeRestaurantes.class);
    var usuarios = mock(RepositorioDeUsuarios.class);
    var execucaoNoTenant = mock(ExecucaoNoTenant.class);
    var hash = mock(HashDeSenha.class);
    var limite = mock(LimiteDeTentativas.class);
    var criador = mock(CriadorDeSessao.class);
    var relogio = mock(Relogio.class);
    Instant agora = Instant.parse("2026-11-05T12:00:00Z");
    when(relogio.agora()).thenReturn(agora);
    when(limite.reservarTentativaPorIp("192.0.2.1", agora)).thenReturn(0L);
    when(limite.tempoRestanteDeBloqueioDaConta("ausente:admin", agora)).thenReturn(0L);
    when(restaurantes.buscarPorSlug("ausente")).thenReturn(Optional.empty());
    when(hash.gerar(anyString())).thenReturn("hash-ficticio");
    when(hash.corresponde("senha-informada", "hash-ficticio")).thenReturn(false);

    var autenticar =
        new Autenticar(restaurantes, usuarios, execucaoNoTenant, hash, limite, criador, relogio);

    assertThatThrownBy(
            () -> autenticar.executar("ausente", "admin", "senha-informada", "192.0.2.1"))
        .isInstanceOf(CredenciaisInvalidas.class);

    verify(hash).corresponde("senha-informada", "hash-ficticio");
    verify(limite).registrarFalhaDaConta("ausente:admin", agora);
  }
}
