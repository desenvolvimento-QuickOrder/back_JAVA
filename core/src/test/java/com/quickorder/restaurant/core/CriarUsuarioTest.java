package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.equipe.application.CampoAtualizacao;
import com.quickorder.restaurant.core.equipe.application.CriarUsuario;
import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionario;
import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionarioDuplicado;
import com.quickorder.restaurant.core.equipe.domain.GeradorDeCodigoFuncionario;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.ConflitoDeNegocio;
import com.quickorder.restaurant.core.shared.error.ExcecaoDeNegocio;
import com.quickorder.restaurant.core.shared.tenant.TenantContext;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CriarUsuarioTest {

  @Test
  void deveSortearOutroCodigoDepoisDeUmaColisaoConcorrente() {
    RepositorioDeUsuarios usuarios = mock(RepositorioDeUsuarios.class);
    HashDeSenha hashDeSenha = mock(HashDeSenha.class);
    TenantContext tenantContext = mock(TenantContext.class);
    GeradorDeCodigoFuncionario gerador = mock(GeradorDeCodigoFuncionario.class);
    UUID restauranteId = UUID.randomUUID();
    AtomicInteger sorteios = new AtomicInteger();

    when(tenantContext.tenantAtual()).thenReturn(Optional.of(restauranteId));
    when(hashDeSenha.gerar("2580")).thenReturn("hash-do-pin");
    when(usuarios.codigosUsados()).thenReturn(List.of(), List.of("0001"));
    when(gerador.sortear(anyList()))
        .thenAnswer(
            chamada -> {
              List<CodigoFuncionario> disponiveis = chamada.getArgument(0);
              assertThat(disponiveis).noneMatch(codigo -> codigo.valor().equals("2580"));
              return new CodigoFuncionario(sorteios.getAndIncrement() == 0 ? "0001" : "0002");
            });
    when(usuarios.salvar(any(Usuario.class)))
        .thenThrow(new CodigoFuncionarioDuplicado())
        .thenAnswer(chamada -> chamada.getArgument(0));

    Usuario criado =
        new CriarUsuario(usuarios, hashDeSenha, tenantContext, gerador)
            .executar(
                "Garçom",
                Papel.GARCOM,
                CampoAtualizacao.ausente(),
                CampoAtualizacao.ausente(),
                CampoAtualizacao.informado("2580"));

    assertThat(criado.codigo()).isEqualTo("0002");
    assertThat(criado.deveTrocarPin()).isTrue();
    verify(usuarios, times(2)).salvar(any(Usuario.class));
    verify(usuarios, times(2)).codigosUsados();
  }

  @Test
  void deveResponderCodigosEsgotadosQuandoTodosOsCodigosJaForamUsados() {
    RepositorioDeUsuarios usuarios = mock(RepositorioDeUsuarios.class);
    HashDeSenha hashDeSenha = mock(HashDeSenha.class);
    TenantContext tenantContext = mock(TenantContext.class);
    GeradorDeCodigoFuncionario gerador = mock(GeradorDeCodigoFuncionario.class);
    UUID restauranteId = UUID.randomUUID();
    List<String> codigosUsados =
        IntStream.range(0, 10_000)
            .mapToObj(numero -> String.format(Locale.ROOT, "%04d", numero))
            .toList();

    when(tenantContext.tenantAtual()).thenReturn(Optional.of(restauranteId));
    when(usuarios.codigosUsados()).thenReturn(codigosUsados);

    Throwable erro =
        catchThrowable(
            () ->
                new CriarUsuario(usuarios, hashDeSenha, tenantContext, gerador)
                    .executar(
                        "Garçom",
                        Papel.GARCOM,
                        CampoAtualizacao.ausente(),
                        CampoAtualizacao.ausente(),
                        CampoAtualizacao.informado("2580")));

    assertThat(erro).isInstanceOf(ExcecaoDeNegocio.class);
    assertThat(((ExcecaoDeNegocio) erro).getCode()).isEqualTo("CODIGOS_ESGOTADOS");
    verify(gerador, never()).sortear(anyList());
  }

  @Test
  void deveLimitarAteTresTentativasQuandoCodigoColideSempre() {
    RepositorioDeUsuarios usuarios = mock(RepositorioDeUsuarios.class);
    HashDeSenha hashDeSenha = mock(HashDeSenha.class);
    TenantContext tenantContext = mock(TenantContext.class);
    GeradorDeCodigoFuncionario gerador = mock(GeradorDeCodigoFuncionario.class);
    UUID restauranteId = UUID.randomUUID();

    when(tenantContext.tenantAtual()).thenReturn(Optional.of(restauranteId));
    when(hashDeSenha.gerar("2580")).thenReturn("hash-do-pin");
    when(usuarios.codigosUsados()).thenReturn(List.of());
    when(gerador.sortear(anyList())).thenReturn(new CodigoFuncionario("0001"));
    when(usuarios.salvar(any(Usuario.class))).thenThrow(new CodigoFuncionarioDuplicado());

    Throwable erro =
        catchThrowable(
            () ->
                new CriarUsuario(usuarios, hashDeSenha, tenantContext, gerador)
                    .executar(
                        "Garçom",
                        Papel.GARCOM,
                        CampoAtualizacao.ausente(),
                        CampoAtualizacao.ausente(),
                        CampoAtualizacao.informado("2580")));

    assertThat(erro).isInstanceOf(ConflitoDeNegocio.class);
    assertThat(((ConflitoDeNegocio) erro).getCode()).isEqualTo("CODIGO_JA_EXISTE");
    verify(usuarios, times(3)).salvar(any(Usuario.class));
    verify(gerador, times(3)).sortear(anyList());
  }
}
