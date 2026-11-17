package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.quickorder.restaurant.core.equipe.domain.GeradorDeTokenOpaco;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeDispositivos;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfiguracaoDeRelogioControlado.class)
class AutenticacaoPinIT extends IntegracaoBaseIT {

  private static final AtomicInteger IP = new AtomicInteger(1);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private HashDeSenha hashDeSenha;
  @Autowired private RepositorioDeUsuarios usuarios;
  @Autowired private RepositorioDeDispositivos dispositivos;
  @Autowired private ExecucaoNoTenant execucaoNoTenant;
  @Autowired private GeradorDeTokenOpaco geradorDeToken;
  @Autowired private RelogioControlado relogio;

  @BeforeEach
  void ajustarRelogio() {
    relogio.ajustar(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
  }

  @Test
  void funcionarioTrocaPinObrigatorioEEntraComNovoPin() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    Usuario funcionario = criarFuncionario(admin, "1234", "2580");
    String accessAdmin = accessToken(login(admin.slug(), admin.login(), admin.senha()));
    DadosDoDispositivo dispositivo = registrarDispositivo(accessAdmin);

    MvcResult primeiroLogin =
        loginPin(admin.slug(), funcionario.codigo(), "2580", dispositivo.token());
    assertThat(primeiroLogin.getResponse().getStatus()).isEqualTo(200);
    var resposta = objectMapper.readTree(primeiroLogin.getResponse().getContentAsString());
    assertThat(resposta.get("deveTrocarPin").asBoolean()).isTrue();
    var jwt = jwtDecoder.decode(resposta.get("accessToken").asString());
    assertThat(jwt.getClaimAsString("did")).isEqualTo(dispositivo.id().toString());
    assertThat(jwt.getClaimAsBoolean("troca_pin_pendente")).isTrue();
    assertThat(
            execucaoNoTenant.executar(
                admin.restauranteId(),
                () -> dispositivos.buscarPorId(dispositivo.id()).orElseThrow().ultimoUsoEm()))
        .isNotNull();

    MvcResult bloqueada =
        mockMvc
            .perform(
                get("/api/v1/mesas")
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + resposta.get("accessToken").asString()))
            .andReturn();
    assertThat(bloqueada.getResponse().getStatus()).isEqualTo(403);
    assertThat(codigo(bloqueada)).isEqualTo("TROCA_DE_PIN_OBRIGATORIA");

    MvcResult pinNovoFraco = trocarPin(resposta.get("accessToken").asString(), "2580", "1234");
    assertThat(pinNovoFraco.getResponse().getStatus()).isEqualTo(400);
    assertThat(pinNovoFraco.getResponse().getContentAsString()).contains("pinNovo");
    MvcResult pinAtualInvalido = trocarPin(resposta.get("accessToken").asString(), "9999", "2581");
    assertThat(pinAtualInvalido.getResponse().getStatus()).isEqualTo(422);
    assertThat(codigo(pinAtualInvalido)).isEqualTo("PIN_ATUAL_INVALIDO");
    assertThat(
            trocarPin(resposta.get("accessToken").asString(), "2580", "2581")
                .getResponse()
                .getStatus())
        .isEqualTo(204);
    assertThat(
            renovar(resposta.get("refreshToken").asString(), dispositivo.token())
                .getResponse()
                .getStatus())
        .isEqualTo(401);
    assertThat(
            loginPin(admin.slug(), funcionario.codigo(), "2580", dispositivo.token())
                .getResponse()
                .getStatus())
        .isEqualTo(401);

    MvcResult novoLogin = loginPin(admin.slug(), funcionario.codigo(), "2581", dispositivo.token());
    assertThat(novoLogin.getResponse().getStatus()).isEqualTo(200);
    var novoCorpo = objectMapper.readTree(novoLogin.getResponse().getContentAsString());
    assertThat(novoCorpo.get("deveTrocarPin").asBoolean()).isFalse();
    assertThat(jwtDecoder.decode(novoCorpo.get("accessToken").asString()).getClaimAsString("did"))
        .isEqualTo(dispositivo.id().toString());
  }

  @Test
  void refreshExigeMesmoDispositivoERevogacaoImpedeLoginERefresh() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    Usuario funcionario = criarFuncionario(admin, "2468", "2580");
    String accessAdmin = accessToken(login(admin.slug(), admin.login(), admin.senha()));
    DadosDoDispositivo dispositivo = registrarDispositivo(accessAdmin);
    MvcResult listagem =
        mockMvc
            .perform(
                get("/api/v1/dispositivos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessAdmin))
            .andReturn();
    assertThat(listagem.getResponse().getStatus()).isEqualTo(200);
    assertThat(listagem.getResponse().getContentAsString())
        .contains(dispositivo.id().toString())
        .doesNotContain(dispositivo.token());
    String refresh =
        objectMapper
            .readTree(
                loginPin(admin.slug(), funcionario.codigo(), "2580", dispositivo.token())
                    .getResponse()
                    .getContentAsString())
            .get("refreshToken")
            .asString();

    assertThat(renovar(refresh, null).getResponse().getStatus()).isEqualTo(401);
    assertThat(renovar(refresh, "token-de-outro-aparelho").getResponse().getStatus())
        .isEqualTo(401);
    MvcResult renovada = renovar(refresh, dispositivo.token());
    assertThat(renovada.getResponse().getStatus()).isEqualTo(200);
    var corpoRenovado = objectMapper.readTree(renovada.getResponse().getContentAsString());
    String novoRefresh = corpoRenovado.get("refreshToken").asString();
    assertThat(
            jwtDecoder.decode(corpoRenovado.get("accessToken").asString()).getClaimAsString("did"))
        .isEqualTo(dispositivo.id().toString());
    assertThat(
            jwtDecoder
                .decode(corpoRenovado.get("accessToken").asString())
                .getClaimAsBoolean("troca_pin_pendente"))
        .isTrue();

    DadosDeAutenticacaoTeste outroRestaurante = criarAdmin();
    String accessOutroAdmin =
        accessToken(
            login(outroRestaurante.slug(), outroRestaurante.login(), outroRestaurante.senha()));
    MvcResult crossTenant = revogar(accessOutroAdmin, dispositivo.id());
    assertThat(crossTenant.getResponse().getStatus()).isEqualTo(404);
    assertThat(codigo(crossTenant)).isEqualTo("RECURSO_NAO_ENCONTRADO");
    assertThat(revogar(accessAdmin, dispositivo.id()).getResponse().getStatus()).isEqualTo(200);
    assertThat(revogar(accessAdmin, dispositivo.id()).getResponse().getStatus()).isEqualTo(200);
    assertThat(renovar(novoRefresh, dispositivo.token()).getResponse().getStatus()).isEqualTo(401);
    assertThat(
            loginPin(admin.slug(), funcionario.codigo(), "2580", dispositivo.token())
                .getResponse()
                .getStatus())
        .isEqualTo(401);
  }

  @Test
  void falhasDePinSaoGenericasEContasSaoLimitadas() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    Usuario funcionario = criarFuncionario(admin, "3579", "2580");
    String accessAdmin = accessToken(login(admin.slug(), admin.login(), admin.senha()));
    DadosDoDispositivo dispositivo = registrarDispositivo(accessAdmin);

    MvcResult semDispositivo = loginPin(admin.slug(), funcionario.codigo(), "2580", null);
    MvcResult dispositivoDesconhecido =
        loginPin(admin.slug(), funcionario.codigo(), "2580", "token-falso");
    MvcResult pinErrado = loginPin(admin.slug(), funcionario.codigo(), "9999", dispositivo.token());
    MvcResult codigoAusente = loginPin(admin.slug(), "9998", "2580", dispositivo.token());
    MvcResult restauranteAusente =
        loginPin("slug-inexistente", funcionario.codigo(), "2580", dispositivo.token());
    assertThat(semDispositivo.getResponse().getStatus()).isEqualTo(401);
    assertThat(pinErrado.getResponse().getStatus()).isEqualTo(401);
    assertThat(codigoAusente.getResponse().getStatus()).isEqualTo(401);
    assertThat(dispositivoDesconhecido.getResponse().getStatus()).isEqualTo(401);
    assertThat(restauranteAusente.getResponse().getStatus()).isEqualTo(401);
    assertThat(codigo(semDispositivo)).isEqualTo("CREDENCIAIS_INVALIDAS");
    assertThat(codigo(pinErrado)).isEqualTo(codigo(semDispositivo));
    assertThat(codigo(dispositivoDesconhecido)).isEqualTo(codigo(semDispositivo));
    assertThat(codigo(restauranteAusente)).isEqualTo(codigo(semDispositivo));
    assertThat(
            objectMapper
                .readTree(pinErrado.getResponse().getContentAsString())
                .get("detail")
                .asString())
        .isEqualTo(
            objectMapper
                .readTree(semDispositivo.getResponse().getContentAsString())
                .get("detail")
                .asString());

    for (int tentativa = 0; tentativa < 3; tentativa++) {
      assertThat(
              loginPin(admin.slug(), funcionario.codigo(), "9999", dispositivo.token())
                  .getResponse()
                  .getStatus())
          .isEqualTo(401);
    }
    MvcResult contaBloqueada =
        loginPin(admin.slug(), funcionario.codigo(), "2580", dispositivo.token());
    assertThat(contaBloqueada.getResponse().getStatus()).isEqualTo(429);
    assertThat(contaBloqueada.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
  }

  @Test
  void vinteFalhasEmUmDispositivoBloqueiamPorDezMinutos() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String accessAdmin = accessToken(login(admin.slug(), admin.login(), admin.senha()));
    DadosDoDispositivo dispositivo = registrarDispositivo(accessAdmin);

    for (int tentativa = 0; tentativa < 20; tentativa++) {
      String codigo = String.format("%04d", 4000 + tentativa);
      assertThat(
              loginPin(admin.slug(), codigo, "2580", dispositivo.token()).getResponse().getStatus())
          .isEqualTo(401);
    }
    MvcResult bloqueada = loginPin(admin.slug(), "4999", "2580", dispositivo.token());
    assertThat(bloqueada.getResponse().getStatus()).isEqualTo(429);
    assertThat(bloqueada.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("600");
  }

  private DadosDeAutenticacaoTeste criarAdmin() throws Exception {
    return DadosDeAutenticacaoTeste.criar(
        usuarios, hashDeSenha, execucaoNoTenant, Papel.ADMIN, true, true);
  }

  private Usuario criarFuncionario(DadosDeAutenticacaoTeste admin, String codigo, String pin) {
    Usuario funcionario =
        Usuario.novoFuncionario(
            UUID.randomUUID(),
            admin.restauranteId(),
            "Funcionário",
            Papel.GARCOM,
            codigo,
            hashDeSenha.gerar(pin));
    execucaoNoTenant.executar(admin.restauranteId(), () -> usuarios.salvar(funcionario));
    return funcionario;
  }

  private DadosDoDispositivo registrarDispositivo(String accessToken) throws Exception {
    MvcResult resultado =
        mockMvc
            .perform(
                post("/api/v1/dispositivos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Tablet\"}"))
            .andReturn();
    assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
    var corpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
    return new DadosDoDispositivo(
        UUID.fromString(corpo.get("id").asString()), corpo.get("deviceToken").asString());
  }

  private MvcResult login(String slug, String login, String senha) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(
                    request -> {
                      request.setRemoteAddr(proximoIp());
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("restaurante", slug, "login", login, "senha", senha))))
        .andReturn();
  }

  private MvcResult loginPin(String slug, String codigo, String pin, String deviceToken)
      throws Exception {
    var requisicao = post("/api/v1/auth/login-pin").contentType(MediaType.APPLICATION_JSON);
    if (deviceToken != null) {
      requisicao.header("X-Device-Token", deviceToken);
    }
    return mockMvc
        .perform(
            requisicao
                .with(
                    request -> {
                      request.setRemoteAddr(proximoIp());
                      return request;
                    })
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("restaurante", slug, "codigo", codigo, "pin", pin))))
        .andReturn();
  }

  private MvcResult renovar(String refreshToken, String deviceToken) throws Exception {
    var requisicao =
        post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken)));
    if (deviceToken != null) {
      requisicao.header("X-Device-Token", deviceToken);
    }
    return mockMvc.perform(requisicao).andReturn();
  }

  private MvcResult trocarPin(String accessToken, String pinAtual, String pinNovo)
      throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/auth/trocar-pin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("pinAtual", pinAtual, "pinNovo", pinNovo))))
        .andReturn();
  }

  private MvcResult revogar(String accessToken, UUID dispositivoId) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/dispositivos/" + dispositivoId + "/revogar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andReturn();
  }

  private String accessToken(MvcResult resultado) throws Exception {
    return objectMapper
        .readTree(resultado.getResponse().getContentAsString())
        .get("accessToken")
        .asString();
  }

  private String codigo(MvcResult resultado) throws Exception {
    return objectMapper
        .readTree(resultado.getResponse().getContentAsString())
        .get("code")
        .asString();
  }

  private static String proximoIp() {
    return "198.51.100." + (IP.getAndIncrement() % 250 + 1);
  }

  private record DadosDoDispositivo(UUID id, String token) {}
}
