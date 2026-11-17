package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.equipe.application.AutenticarPorPin;
import com.quickorder.restaurant.core.equipe.application.CriadorDeSessao;
import com.quickorder.restaurant.core.equipe.domain.CredenciaisInvalidas;
import com.quickorder.restaurant.core.equipe.domain.Dispositivo;
import com.quickorder.restaurant.core.equipe.domain.GeradorDeTokenOpaco;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativas;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeDispositivos;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeRestaurantes;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Restaurante;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class AutenticarPorPinTest {

  @Test
  void deveRegistrarFalhaDoDispositivoPeloIdDoDispositivoValido() {
    var restaurantes = mock(RepositorioDeRestaurantes.class);
    var usuarios = mock(RepositorioDeUsuarios.class);
    var dispositivos = mock(RepositorioDeDispositivos.class);
    var execucaoNoTenant = mock(ExecucaoNoTenant.class);
    var hash = mock(HashDeSenha.class);
    var geradorDeToken = mock(GeradorDeTokenOpaco.class);
    var limite = mock(LimiteDeTentativas.class);
    var criador = mock(CriadorDeSessao.class);
    var relogio = mock(Relogio.class);
    UUID restauranteId = UUID.randomUUID();
    UUID dispositivoId = UUID.randomUUID();
    Instant agora = Instant.parse("2026-11-05T12:00:00Z");
    var restaurante = new Restaurante(restauranteId, "restaurante", true);
    var dispositivo =
        new Dispositivo(
            dispositivoId,
            restauranteId,
            "Tablet",
            "hash-do-token",
            UUID.randomUUID(),
            agora,
            null,
            null);
    var usuario =
        Usuario.novoFuncionario(
            UUID.randomUUID(), restauranteId, "Garçom", Papel.GARCOM, "1234", "hash-do-pin");
    when(relogio.agora()).thenReturn(agora);
    when(restaurantes.buscarPorSlug("restaurante")).thenReturn(Optional.of(restaurante));
    when(geradorDeToken.hash("token-do-dispositivo")).thenReturn("hash-do-token");
    when(dispositivos.buscarPorHashDoToken("hash-do-token")).thenReturn(Optional.of(dispositivo));
    when(usuarios.buscarPorCodigo("1234")).thenReturn(Optional.of(usuario));
    when(hash.gerar(anyString())).thenReturn("hash-ficticio");
    when(hash.corresponde("9999", "hash-do-pin")).thenReturn(false);
    doAnswer(invocacao -> ((Supplier<?>) invocacao.getArgument(1)).get())
        .when(execucaoNoTenant)
        .executar(eq(restauranteId), any());

    var autenticar =
        new AutenticarPorPin(
            restaurantes,
            usuarios,
            dispositivos,
            execucaoNoTenant,
            hash,
            geradorDeToken,
            limite,
            criador,
            relogio);

    assertThatThrownBy(
            () ->
                autenticar.executar(
                    "restaurante", "1234", "9999", "token-do-dispositivo", "192.0.2.1"))
        .isInstanceOf(CredenciaisInvalidas.class);

    verify(limite).registrarFalhaDaConta("pin:restaurante:1234", agora);
    verify(limite).registrarFalhaDoDispositivo(dispositivoId.toString(), agora);
    verify(limite, never()).registrarFalhaDoDispositivo("hash-do-token", agora);
  }
}
