package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.quickorder.restaurant.core.equipe.domain.GeradorDeTokenOpaco;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.security.interfaces.RSAPrivateKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfiguracaoDeRelogioControlado.class)
class AutenticacaoIT extends IntegracaoBaseIT {

  private static final AtomicInteger IP_SEQUENCIAL = new AtomicInteger(1);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private HashDeSenha hashDeSenha;
  @Autowired private RepositorioDeUsuarios usuarios;
  @Autowired private GeradorDeTokenOpaco geradorDeToken;
  @Autowired private ExecucaoNoTenant execucaoNoTenant;
  @Autowired private RelogioControlado relogio;

  @BeforeEach
  void ajustarRelogio() {
    relogio.ajustar(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
  }

  @Test
  void adminAtivoEntraComAccessTokenERefreshToken() throws Exception {
    DadosDeAutenticacaoTeste dados = criarAdmin();

    MvcResult resultado = login(dados.slug(), dados.login(), dados.senha());

    assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    var corpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
    String accessToken = corpo.get("accessToken").asString();
    assertThat(corpo.get("refreshToken").asString()).isNotBlank();
    assertThat(corpo.get("tokenType").asString()).isEqualTo("Bearer");
    assertThat(corpo.get("expiresIn").asInt()).isEqualTo(900);

    var jwt = jwtDecoder.decode(accessToken);
    assertThat(jwt.getSubject()).isEqualTo(dados.usuarioId().toString());
    assertThat(jwt.getClaimAsString("tenant_id")).isEqualTo(dados.restauranteId().toString());
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
    assertThat(jwt.getIssuer()).hasToString("https://quickorder");
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds()).isEqualTo(900);
    assertThat(jwt.getHeaders()).containsEntry("kid", "teste-atual");
  }

  @Test
  void falhasDeLoginRespondemComAsMesmasCredenciaisInvalidas() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    DadosDeAutenticacaoTeste inativo = criarUsuario(Papel.ADMIN, false, true);
    DadosDeAutenticacaoTeste garcom = criarUsuario(Papel.GARCOM, true, true);
    DadosDeAutenticacaoTeste restauranteInativo = criarUsuario(Papel.ADMIN, true, false);

    List<MvcResult> resultados =
        List.of(
            login(admin.slug(), admin.login(), "senha-errada"),
            login(admin.slug(), "login-inexistente", admin.senha()),
            login("restaurante-inexistente", admin.login(), admin.senha()),
            login(inativo.slug(), inativo.login(), inativo.senha()),
            login(garcom.slug(), garcom.login(), garcom.senha()),
            login(
                restauranteInativo.slug(), restauranteInativo.login(), restauranteInativo.senha()));

    for (MvcResult resultado : resultados) {
      assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
      var corpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
      assertThat(corpo.get("code").asString()).isEqualTo("CREDENCIAIS_INVALIDAS");
      assertThat(corpo.get("detail").asString()).isEqualTo("Credenciais inválidas.");
      assertThat(resultado.getResponse().getContentAsString())
          .doesNotContain(admin.senha(), inativo.senha(), garcom.senha());
    }
  }

  @Test
  void mesmoLoginEmRestaurantesDiferentesRecebeOTenantCorreto() throws Exception {
    DadosDeAutenticacaoTeste primeiro = criarAdmin();
    DadosDeAutenticacaoTeste segundo = criarAdmin();

    var jwtPrimeiro =
        jwtDecoder.decode(accessToken(login(primeiro.slug(), primeiro.login(), primeiro.senha())));
    var jwtSegundo =
        jwtDecoder.decode(accessToken(login(segundo.slug(), segundo.login(), segundo.senha())));

    assertThat(jwtPrimeiro.getSubject()).isEqualTo(primeiro.usuarioId().toString());
    assertThat(jwtPrimeiro.getClaimAsString("tenant_id"))
        .isEqualTo(primeiro.restauranteId().toString());
    assertThat(jwtSegundo.getSubject()).isEqualTo(segundo.usuarioId().toString());
    assertThat(jwtSegundo.getClaimAsString("tenant_id"))
        .isEqualTo(segundo.restauranteId().toString());
  }

  @Test
  void cincoFalhasBloqueiamAContaMesmoQuandoOsIpsMudam() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();

    for (int tentativa = 0; tentativa < 5; tentativa++) {
      assertThat(
              loginDeIp(admin.slug(), admin.login(), "senha-errada", proximoIp())
                  .getResponse()
                  .getStatus())
          .isEqualTo(401);
    }

    MvcResult bloqueada = loginDeIp(admin.slug(), admin.login(), admin.senha(), proximoIp());
    assertThat(bloqueada.getResponse().getStatus()).isEqualTo(429);
    assertThat(bloqueada.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
    assertThat(
            objectMapper
                .readTree(bloqueada.getResponse().getContentAsString())
                .get("code")
                .asString())
        .isEqualTo("LIMITE_EXCEDIDO");
  }

  @Test
  void loginBemSucedidoReiniciaOContadorDeFalhasDaConta() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();

    for (int tentativa = 0; tentativa < 4; tentativa++) {
      assertThat(
              loginDeIp(admin.slug(), admin.login(), "senha-errada", proximoIp())
                  .getResponse()
                  .getStatus())
          .isEqualTo(401);
    }
    assertThat(login(admin.slug(), admin.login(), admin.senha()).getResponse().getStatus())
        .isEqualTo(200);

    for (int tentativa = 0; tentativa < 5; tentativa++) {
      assertThat(
              loginDeIp(admin.slug(), admin.login(), "senha-errada", proximoIp())
                  .getResponse()
                  .getStatus())
          .isEqualTo(401);
    }
    assertThat(
            loginDeIp(admin.slug(), admin.login(), admin.senha(), proximoIp())
                .getResponse()
                .getStatus())
        .isEqualTo(429);
  }

  @Test
  void trintaTentativasPorIpGeram429ComRetryAfter() throws Exception {
    for (int tentativa = 0; tentativa < 30; tentativa++) {
      String slug = "restaurante-ausente-" + tentativa;
      assertThat(loginDeIp(slug, "admin", "senha-invalida", "192.0.2.99").getResponse().getStatus())
          .isEqualTo(401);
    }

    MvcResult bloqueada =
        loginDeIp("restaurante-ausente-final", "admin", "senha-invalida", "192.0.2.99");
    assertThat(bloqueada.getResponse().getStatus()).isEqualTo(429);
    assertThat(Long.parseLong(bloqueada.getResponse().getHeader(HttpHeaders.RETRY_AFTER)))
        .isPositive();
  }

  @Test
  void papelCopaNaoPodeCriarMesaERespostaTrazSemPermissao() throws Exception {
    MvcResult resultado =
        mockMvc
            .perform(
                post("/api/v1/mesas")
                    .with(
                        jwt()
                            .jwt(
                                token ->
                                    token
                                        .claim("roles", List.of("COPA"))
                                        .claim("tenant_id", "00000000-0000-0000-0000-000000000001"))
                            .authorities(new SimpleGrantedAuthority("ROLE_COPA")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andReturn();

    assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    assertThat(
            objectMapper
                .readTree(resultado.getResponse().getContentAsString())
                .get("code")
                .asString())
        .isEqualTo("SEM_PERMISSAO");
    assertThat(resultado.getResponse().getHeader("X-Correlation-Id")).isNotBlank();
  }

  @Test
  void refreshRotacionaEReusoAposTrintaSegundosRevogaAFamilia() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String refreshInicial = refreshToken(login(admin.slug(), admin.login(), admin.senha()));
    String refreshRotacionado = refreshToken(renovar(refreshInicial));

    relogio.avancar(Duration.ofSeconds(31));
    assertThat(renovar(refreshInicial).getResponse().getStatus()).isEqualTo(401);
    MvcResult refreshDaFamiliaRevogada = renovar(refreshRotacionado);
    assertThat(refreshDaFamiliaRevogada.getResponse().getStatus()).isEqualTo(401);
    assertThat(
            objectMapper
                .readTree(refreshDaFamiliaRevogada.getResponse().getContentAsString())
                .get("code")
                .asString())
        .isEqualTo("NAO_AUTENTICADO");
  }

  @Test
  void reusoDentroDeTrintaSegundosPreservaAFamiliaDeTokens() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String tokenInicial = refreshToken(login(admin.slug(), admin.login(), admin.senha()));
    String tokenRotacionado = refreshToken(renovar(tokenInicial));
    relogio.avancar(Duration.ofSeconds(29));
    String tokenConcorrente = refreshToken(renovar(tokenInicial));

    assertThat(familiaId(tokenRotacionado))
        .isEqualTo(familiaId(tokenInicial))
        .isEqualTo(familiaId(tokenConcorrente));
    assertThat(renovar(tokenRotacionado).getResponse().getStatus()).isEqualTo(200);
  }

  @Test
  void rotacaoNaoEstendeAValidadeAbsolutaDeDozeHorasDaFamilia() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String tokenInicial = refreshToken(login(admin.slug(), admin.login(), admin.senha()));

    relogio.avancar(Duration.ofHours(11));
    String tokenRotacionado = refreshToken(renovar(tokenInicial));
    relogio.avancar(Duration.ofHours(1).plusSeconds(1));

    assertThat(renovar(tokenRotacionado).getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void reusoConcorrenteDentroDaJanelaGeraDoisParesSemRevogarAFamilia() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String refreshInicial = refreshToken(login(admin.slug(), admin.login(), admin.senha()));
    var inicio = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(2)) {
      var primeiro =
          executor.submit(
              () -> {
                inicio.await();
                return renovar(refreshInicial).getResponse().getStatus();
              });
      var segundo =
          executor.submit(
              () -> {
                inicio.await();
                return renovar(refreshInicial).getResponse().getStatus();
              });
      inicio.countDown();

      assertThat(primeiro.get()).isEqualTo(200);
      assertThat(segundo.get()).isEqualTo(200);
    }
  }

  @Test
  void logoutEIdempotenteRevogaRefreshMasNaoRevogaAccessToken() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    MvcResult login = login(admin.slug(), admin.login(), admin.senha());
    String accessToken = accessToken(login);
    String refreshToken = refreshToken(login);

    assertThat(encerrar("token-desconhecido").getResponse().getStatus()).isEqualTo(204);
    assertThat(encerrar(refreshToken).getResponse().getStatus()).isEqualTo(204);
    assertThat(encerrar(refreshToken).getResponse().getStatus()).isEqualTo(204);
    assertThat(renovar(refreshToken).getResponse().getStatus()).isEqualTo(401);

    MvcResult accessAindaValido =
        mockMvc
            .perform(
                get("/api/v1/mesas").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andReturn();
    assertThat(accessAindaValido.getResponse().getStatus()).isEqualTo(404);
  }

  @Test
  void logoutAposExpiracaoDoAccessTokenRevogaRefreshAindaValido() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    MvcResult login = login(admin.slug(), admin.login(), admin.senha());
    Instant expiracaoDoAccess = jwtDecoder.decode(accessToken(login)).getExpiresAt();
    String refreshToken = refreshToken(login);

    relogio.ajustar(expiracaoDoAccess.plusSeconds(1));

    assertThat(relogio.agora()).isAfter(expiracaoDoAccess);
    assertThat(encerrar(refreshToken).getResponse().getStatus()).isEqualTo(204);
    assertThat(renovar(refreshToken).getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void logoutDeRefreshExpiradoContinuaRespondendo204() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String refreshToken = refreshToken(login(admin.slug(), admin.login(), admin.senha()));

    relogio.avancar(Duration.ofHours(13));

    assertThat(encerrar(refreshToken).getResponse().getStatus()).isEqualTo(204);
    assertThat(renovar(refreshToken).getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void refreshRejeitaUsuarioOuRestauranteInativo() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String refreshAdmin = refreshToken(login(admin.slug(), admin.login(), admin.senha()));
    execucaoNoTenant.executar(
        admin.restauranteId(),
        () ->
            usuarios.salvar(
                new Usuario(
                    admin.usuarioId(),
                    admin.restauranteId(),
                    admin.login(),
                    admin.login(),
                    hashDeSenha.gerar(admin.senha()),
                    Papel.ADMIN,
                    false)));
    assertThat(renovar(refreshAdmin).getResponse().getStatus()).isEqualTo(401);

    DadosDeAutenticacaoTeste outroAdmin = criarAdmin();
    String refreshRestaurante =
        refreshToken(login(outroAdmin.slug(), outroAdmin.login(), outroAdmin.senha()));
    DadosDeAutenticacaoTeste.definirRestauranteAtivo(outroAdmin.restauranteId(), false);
    assertThat(renovar(refreshRestaurante).getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void tokenExpiradoAlteradoOuAssinadoPorOutraChaveRecebe401() throws Exception {
    Instant agora = relogio.agora();

    String tokenExpirado =
        assinarToken(
            chavePrivadaAtual(),
            "teste-atual",
            agora.minus(Duration.ofHours(1)),
            agora.minus(Duration.ofMinutes(30)),
            "https://quickorder");
    String tokenValido = assinarToken(chavePrivadaAtual(), "teste-atual", agora);
    String tokenAlterado = alterarAssinatura(tokenValido);
    String tokenOutraChave = assinarToken(chavePrivadaAnterior(), "teste-anterior", agora);
    String tokenEmissorInvalido =
        assinarToken(
            chavePrivadaAtual(),
            "teste-atual",
            agora,
            agora.plus(Duration.ofMinutes(15)),
            "https://emissor-invalido");

    for (String token :
        List.of(tokenExpirado, tokenAlterado, tokenOutraChave, tokenEmissorInvalido)) {
      MvcResult resultado =
          mockMvc
              .perform(get("/api/v1/mesas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
              .andReturn();
      assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
      assertThat(
              objectMapper
                  .readTree(resultado.getResponse().getContentAsString())
                  .get("code")
                  .asString())
          .isEqualTo("NAO_AUTENTICADO");
    }
  }

  private DadosDeAutenticacaoTeste criarAdmin() throws Exception {
    return criarUsuario(Papel.ADMIN, true, true);
  }

  private DadosDeAutenticacaoTeste criarUsuario(
      Papel papel, boolean usuarioAtivo, boolean restauranteAtivo) throws Exception {
    return DadosDeAutenticacaoTeste.criar(
        usuarios, hashDeSenha, execucaoNoTenant, papel, usuarioAtivo, restauranteAtivo);
  }

  private MvcResult login(String slug, String login, String senha) throws Exception {
    return loginDeIp(slug, login, senha, proximoIp());
  }

  private MvcResult loginDeIp(String slug, String login, String senha, String ip) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(
                    request -> {
                      request.setRemoteAddr(ip);
                      return request;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"restaurante\":\""
                        + slug
                        + "\",\"login\":\""
                        + login
                        + "\",\"senha\":\""
                        + senha
                        + "\"}"))
        .andReturn();
  }

  private MvcResult renovar(String refreshToken) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andReturn();
  }

  private MvcResult encerrar(String refreshToken) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andReturn();
  }

  private String accessToken(MvcResult resultado) throws Exception {
    return objectMapper
        .readTree(resultado.getResponse().getContentAsString())
        .get("accessToken")
        .asString();
  }

  private String refreshToken(MvcResult resultado) throws Exception {
    return objectMapper
        .readTree(resultado.getResponse().getContentAsString())
        .get("refreshToken")
        .asString();
  }

  private static String proximoIp() {
    return "198.51.100." + IP_SEQUENCIAL.getAndIncrement();
  }

  private String assinarToken(RSAPrivateKey chavePrivada, String kid, Instant instante)
      throws Exception {
    return assinarToken(
        chavePrivada, kid, instante, instante.plus(Duration.ofMinutes(15)), "https://quickorder");
  }

  private String assinarToken(
      RSAPrivateKey chavePrivada, String kid, Instant emitidoEm, Instant expiracao, String emissor)
      throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(emissor)
            .subject(UUID.randomUUID().toString())
            .issueTime(Date.from(emitidoEm))
            .expirationTime(Date.from(expiracao))
            .jwtID(UUID.randomUUID().toString())
            .claim("tenant_id", UUID.randomUUID().toString())
            .claim("roles", List.of("ADMIN"))
            .build();
    SignedJWT token =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).type(JOSEObjectType.JWT).build(),
            claims);
    token.sign(new RSASSASigner(chavePrivada));
    return token.serialize();
  }

  private static String alterarAssinatura(String token) {
    int inicioAssinatura = token.lastIndexOf('.') + 1;
    char primeiroCaractere = token.charAt(inicioAssinatura);
    char substituto = primeiroCaractere == 'A' ? 'B' : 'A';
    return token.substring(0, inicioAssinatura)
        + substituto
        + token.substring(inicioAssinatura + 1);
  }

  private RSAPrivateKey chavePrivadaAtual() throws Exception {
    try (var entrada = new ClassPathResource("keys/test-private.pem").getInputStream()) {
      return RsaKeyConverters.pkcs8().convert(entrada);
    }
  }

  private RSAPrivateKey chavePrivadaAnterior() throws Exception {
    try (var entrada = new ClassPathResource("keys/test-second-private.pem").getInputStream()) {
      return RsaKeyConverters.pkcs8().convert(entrada);
    }
  }

  private UUID familiaId(String refreshToken) throws SQLException {
    try (Connection conexao =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        PreparedStatement comando =
            conexao.prepareStatement("select familia_id from refresh_token where token_hash = ?")) {
      comando.setString(1, geradorDeToken.hash(refreshToken));
      try (var resultado = comando.executeQuery()) {
        assertThat(resultado.next()).isTrue();
        return resultado.getObject("familia_id", UUID.class);
      }
    }
  }
}
