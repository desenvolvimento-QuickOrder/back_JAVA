package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.equipe.application.AtualizarUsuario;
import com.quickorder.restaurant.core.equipe.application.CampoAtualizacao;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.ErroDeValidacaoDaRequisicao;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AtualizarUsuarioTest {

  @Test
  void deveRejeitarPinIgualAoCodigoDoFuncionario() {
    RepositorioDeUsuarios usuarios = mock(RepositorioDeUsuarios.class);
    RepositorioDeSessoesDeRefresh sessoes = mock(RepositorioDeSessoesDeRefresh.class);
    HashDeSenha hashDeSenha = mock(HashDeSenha.class);
    Relogio relogio = mock(Relogio.class);
    UUID restauranteId = UUID.randomUUID();
    UUID funcionarioId = UUID.randomUUID();
    Usuario funcionario =
        Usuario.novoFuncionario(
            funcionarioId, restauranteId, "Garçom", Papel.GARCOM, "2580", "hash-pin");
    when(usuarios.buscarPorId(funcionarioId)).thenReturn(Optional.of(funcionario));

    Throwable erro =
        catchThrowable(
            () ->
                new AtualizarUsuario(usuarios, sessoes, hashDeSenha, relogio)
                    .executar(
                        funcionarioId,
                        UUID.randomUUID(),
                        CampoAtualizacao.ausente(),
                        CampoAtualizacao.ausente(),
                        CampoAtualizacao.ausente(),
                        CampoAtualizacao.ausente(),
                        CampoAtualizacao.informado("2580")));

    assertThat(erro).isInstanceOf(ErroDeValidacaoDaRequisicao.class);
  }
}
