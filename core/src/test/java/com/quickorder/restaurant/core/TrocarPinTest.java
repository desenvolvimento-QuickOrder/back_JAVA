package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.equipe.application.TrocarPin;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.SessaoInvalida;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class TrocarPinTest {

  @Test
  void deveInvalidarSessaoQuandoUsuarioNaoExisteNoTenant() {
    var usuarios = mock(RepositorioDeUsuarios.class);
    var sessoes = mock(RepositorioDeSessoesDeRefresh.class);
    var hash = mock(HashDeSenha.class);
    var execucaoNoTenant = mock(ExecucaoNoTenant.class);
    var relogio = mock(Relogio.class);
    UUID restauranteId = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();
    when(usuarios.buscarPorId(usuarioId)).thenReturn(Optional.empty());
    doAnswer(invocacao -> ((Supplier<?>) invocacao.getArgument(1)).get())
        .when(execucaoNoTenant)
        .executar(eq(restauranteId), any());

    var trocarPin = new TrocarPin(usuarios, sessoes, hash, execucaoNoTenant, relogio);

    assertThatThrownBy(() -> trocarPin.executar(restauranteId, usuarioId, "2580", "3579"))
        .isInstanceOf(SessaoInvalida.class);
  }
}
