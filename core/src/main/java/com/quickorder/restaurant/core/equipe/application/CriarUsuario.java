package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionario;
import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionarioDuplicado;
import com.quickorder.restaurant.core.equipe.domain.GeradorDeCodigoFuncionario;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Pin;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.ConflitoDeNegocio;
import com.quickorder.restaurant.core.shared.error.ErroDeCampo;
import com.quickorder.restaurant.core.shared.error.ErroDeValidacaoDaRequisicao;
import com.quickorder.restaurant.core.shared.error.ExcecaoDeNegocio;
import com.quickorder.restaurant.core.shared.tenant.TenantContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class CriarUsuario {

  private static final Pattern LOGIN_VALIDO = Pattern.compile("^[a-z0-9._-]{3,80}$");
  private static final int MAX_TENTATIVAS_DE_CODIGO = 3;
  private static final List<CodigoFuncionario> TODOS_OS_CODIGOS =
      IntStream.range(0, 10_000)
          .mapToObj(numero -> new CodigoFuncionario(String.format(Locale.ROOT, "%04d", numero)))
          .toList();

  private final RepositorioDeUsuarios usuarios;
  private final HashDeSenha hashDeSenha;
  private final TenantContext tenantContext;
  private final GeradorDeCodigoFuncionario geradorDeCodigo;

  public CriarUsuario(
      RepositorioDeUsuarios usuarios,
      HashDeSenha hashDeSenha,
      TenantContext tenantContext,
      GeradorDeCodigoFuncionario geradorDeCodigo) {
    this.usuarios = usuarios;
    this.hashDeSenha = hashDeSenha;
    this.tenantContext = tenantContext;
    this.geradorDeCodigo = geradorDeCodigo;
  }

  public Usuario executar(
      String nome,
      Papel papel,
      CampoAtualizacao<String> login,
      CampoAtualizacao<String> senha,
      CampoAtualizacao<String> pin) {
    List<ErroDeCampo> erros = new ArrayList<>();
    if (nome == null || nome.isBlank() || nome.length() > 120) {
      erros.add(new ErroDeCampo("nome", "Nome obrigatório, com até 120 caracteres."));
    }
    if (papel == null) {
      erros.add(new ErroDeCampo("papel", "Papel obrigatório."));
    } else if (papel == Papel.ADMIN) {
      validarAdmin(login, senha, pin, erros);
    } else if (papel == Papel.GARCOM || papel == Papel.COPA) {
      validarFuncionario(login, senha, pin, erros);
    } else {
      erros.add(new ErroDeCampo("papel", "Papel informado inválido."));
    }
    if (!erros.isEmpty()) {
      throw new ErroDeValidacaoDaRequisicao(erros);
    }

    UUID restauranteId = restauranteAtual();
    if (papel == Papel.ADMIN) {
      String loginNormalizado = login.valor().trim().toLowerCase(Locale.ROOT);
      if (usuarios.existeLogin(loginNormalizado)) {
        throw new ConflitoDeNegocio("LOGIN_JA_EXISTE", "O login já existe neste restaurante.");
      }
      return usuarios.salvar(
          Usuario.novoAdmin(
              UUID.randomUUID(),
              restauranteId,
              nome,
              loginNormalizado,
              hashDeSenha.gerar(senha.valor())));
    }
    return criarFuncionario(restauranteId, nome, papel, new Pin(pin.valor()));
  }

  private Usuario criarFuncionario(UUID restauranteId, String nome, Papel papel, Pin pin) {
    String pinHash = hashDeSenha.gerar(pin.valor());
    for (int tentativa = 0; tentativa < MAX_TENTATIVAS_DE_CODIGO; tentativa++) {
      List<CodigoFuncionario> codigosDisponiveis = codigosDisponiveis(pin);
      if (codigosDisponiveis.isEmpty()) {
        throw codigosEsgotados();
      }
      CodigoFuncionario codigo = geradorDeCodigo.sortear(codigosDisponiveis);
      if (!codigosDisponiveis.contains(codigo)) {
        throw new IllegalStateException("O gerador retornou um código indisponível.");
      }
      Usuario funcionario =
          Usuario.novoFuncionario(
              UUID.randomUUID(), restauranteId, nome, papel, codigo.valor(), pinHash);
      try {
        return usuarios.salvar(funcionario);
      } catch (CodigoFuncionarioDuplicado excecao) {
        if (tentativa + 1 == MAX_TENTATIVAS_DE_CODIGO) {
          if (codigosDisponiveis(pin).isEmpty()) {
            throw codigosEsgotados();
          }
          throw new ConflitoDeNegocio(
              "CODIGO_JA_EXISTE", "Não foi possível reservar um código de funcionário.");
        }
      }
    }
    throw new IllegalStateException("O limite de tentativas de código foi excedido.");
  }

  private List<CodigoFuncionario> codigosDisponiveis(Pin pin) {
    Set<String> usados = new HashSet<>(usuarios.codigosUsados());
    return TODOS_OS_CODIGOS.stream()
        .filter(codigo -> !usados.contains(codigo.valor()))
        .filter(codigo -> !codigo.valor().equals(pin.valor()))
        .toList();
  }

  private UUID restauranteAtual() {
    return tenantContext
        .tenantAtual()
        .orElseThrow(() -> new IllegalStateException("Tenant ausente no contexto."));
  }

  private static void validarAdmin(
      CampoAtualizacao<String> login,
      CampoAtualizacao<String> senha,
      CampoAtualizacao<String> pin,
      List<ErroDeCampo> erros) {
    if (!login.informado()
        || login.valor() == null
        || !LOGIN_VALIDO.matcher(login.valor().trim()).matches()) {
      erros.add(new ErroDeCampo("login", "Login obrigatório, com 3 a 80 caracteres válidos."));
    }
    if (!senha.informado()
        || senha.valor() == null
        || senha.valor().length() < 8
        || senha.valor().length() > 128) {
      erros.add(new ErroDeCampo("senha", "Senha obrigatória, com 8 a 128 caracteres."));
    }
    if (pin.informado()) {
      erros.add(new ErroDeCampo("pin", "ADMIN não pode ter PIN."));
    }
  }

  private static void validarFuncionario(
      CampoAtualizacao<String> login,
      CampoAtualizacao<String> senha,
      CampoAtualizacao<String> pin,
      List<ErroDeCampo> erros) {
    if (login.informado()) {
      erros.add(new ErroDeCampo("login", "GARCOM e COPA não podem ter login."));
    }
    if (senha.informado()) {
      erros.add(new ErroDeCampo("senha", "GARCOM e COPA não podem ter senha."));
    }
    if (!pin.informado() || pin.valor() == null) {
      erros.add(new ErroDeCampo("pin", "PIN obrigatório para GARCOM e COPA."));
      return;
    }
    try {
      if (!new Pin(pin.valor()).podeSerUsado()) {
        erros.add(new ErroDeCampo("pin", "O PIN escolhido não pode ser usado."));
      }
    } catch (IllegalArgumentException excecao) {
      erros.add(new ErroDeCampo("pin", "PIN deve conter quatro dígitos."));
    }
  }

  private static ExcecaoDeNegocio codigosEsgotados() {
    return new ExcecaoDeNegocio(
        "CODIGOS_ESGOTADOS", "Não há códigos de funcionário disponíveis neste restaurante.");
  }
}
