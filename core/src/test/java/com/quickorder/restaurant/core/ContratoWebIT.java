package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickorder.restaurant.testweb.ControladorDeErrosTeste;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ControladorDeErrosTeste.class, ConfiguracaoWebTeste.class})
class ContratoWebIT extends IntegracaoBaseIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtDecoder jwtDecoder;

  private DadosDeIsolamentoTeste dados;

  @BeforeEach
  void prepararRestaurantes() throws Exception {
    dados = DadosDeIsolamentoTeste.inserirRestaurantesERegistros();
  }

  @Test
  void deveGerarCorrelationIdEIncluiLoNaRespostaDeErro() throws Exception {
    var resultado =
        mockMvc
            .perform(get("/teste/erros/negocio"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.correlationId").exists())
            .andReturn();

    String correlationId = resultado.getResponse().getHeader("X-Correlation-Id");
    assertThat(UUID.fromString(correlationId)).isNotNull();
    assertThat(resultado.getResponse().getContentAsString()).contains(correlationId);
  }

  @Test
  void devePreservarCorrelationIdValidoNaRespostaDeErro() throws Exception {
    mockMvc
        .perform(get("/teste/erros/negocio").header("X-Correlation-Id", "abc"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(header().string("X-Correlation-Id", "abc"))
        .andExpect(jsonPath("$.correlationId").value("abc"))
        .andExpect(jsonPath("$.code").value("REGRA_TESTE"));
  }

  @Test
  void deveSubstituirCorrelationIdInvalidoOuMaiorQue64Caracteres() throws Exception {
    for (String invalido : List.of("com espaço", "x".repeat(65))) {
      var resultado =
          mockMvc
              .perform(get("/teste/erros/negocio").header("X-Correlation-Id", invalido))
              .andExpect(status().isUnprocessableEntity())
              .andReturn();
      String correlationId = resultado.getResponse().getHeader("X-Correlation-Id");
      assertThat(UUID.fromString(correlationId)).isNotNull();
      assertThat(correlationId).isNotEqualTo(invalido);
    }
  }

  @Test
  void deveResponderErroInesperadoSemDetalheInterno() throws Exception {
    var resultado =
        mockMvc
            .perform(get("/teste/erros/inesperado"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("ERRO_INTERNO"))
            .andReturn();

    assertThat(resultado.getResponse().getContentAsString())
        .doesNotContain("detalhe interno confidencial", "stackTrace", "exception");
  }

  @Test
  void deveMapearRecursoInexistentePara404() throws Exception {
    mockMvc
        .perform(get("/teste/erros/nao-encontrado"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
  }

  @Test
  void deveMapearConflitosDeNegocioEDeVersaoPara409() throws Exception {
    mockMvc
        .perform(get("/teste/erros/conflito"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLITO_TESTE"));

    mockMvc
        .perform(get("/teste/erros/conflito-de-versao"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLITO_DE_VERSAO"));
  }

  @Test
  void deveResponder500QuandoConstraintNaoPossuiTraducao() throws Exception {
    mockMvc
        .perform(get("/teste/erros/constraint-desconhecida"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("ERRO_INTERNO"));
  }

  @Test
  void deveResponder401ProblemJsonQuandoNaoHaToken() throws Exception {
    mockMvc
        .perform(get("/teste/seguranca/protegido"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("NAO_AUTENTICADO"))
        .andExpect(jsonPath("$.correlationId").exists());
  }

  @Test
  void deveResponder403ComCorrelationIdQuandoPapelNaoEstaAutorizado() throws Exception {
    mockMvc
        .perform(
            get("/teste/seguranca/protegido")
                .with(jwt().jwt(token -> token.claim("roles", List.of("ATENDENTE"))))
                .header("X-Correlation-Id", "abc"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("SEM_PERMISSAO"))
        .andExpect(jsonPath("$.correlationId").value("abc"));
  }

  @Test
  void devePermitirAcessoQuandoTokenContemPapelAdministrativo() throws Exception {
    mockMvc
        .perform(
            get("/teste/seguranca/protegido")
                .with(
                    jwt()
                        .jwt(
                            token ->
                                token
                                    .claim("roles", List.of("ADMIN"))
                                    .claim("tenant_id", dados.restauranteA().toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }

  @Test
  void deveValidarJwtRs256ComOParDeChavesDeTeste() throws Exception {
    String token = assinarTokenDeTeste();

    assertThat(jwtDecoder.decode(token).getSubject()).isEqualTo("usuario-teste");
  }

  @Test
  void deveRejeitarJsonMalformadoUuidInvalidoMetodoEFormatoNaoSuportados() throws Exception {
    mockMvc
        .perform(post("/teste/erros/json").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"));

    mockMvc
        .perform(get("/teste/erros/uuid/nao-e-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"));

    mockMvc
        .perform(put("/teste/erros/negocio"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METODO_NAO_SUPORTADO"));

    mockMvc
        .perform(post("/teste/erros/json").contentType(MediaType.TEXT_PLAIN).content("nome"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("MIDIA_NAO_SUPORTADA"));
  }

  @Test
  void deveTraduzirDuplicidadeRegistradaParaConflitoDeNegocio() throws Exception {
    mockMvc
        .perform(
            post(
                    "/teste/erros/duplicidade/{id}/{restauranteId}",
                    dados.registroA(),
                    dados.restauranteA())
                .with(
                    jwt().jwt(token -> token.claim("tenant_id", dados.restauranteA().toString()))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REGISTRO_DUPLICADO"));
  }

  @Test
  void deveAceitarPreflightDeOrigemPermitidaERejeitarOrigemNaoPermitida() throws Exception {
    mockMvc
        .perform(
            options("/teste/erros/negocio")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
        .andExpect(header().exists("Access-Control-Allow-Methods"));

    mockMvc
        .perform(
            options("/teste/erros/negocio")
                .header("Origin", "https://malicioso.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  private String assinarTokenDeTeste() throws Exception {
    byte[] chavePrivadaPem;
    try (var recurso = getClass().getClassLoader().getResourceAsStream("keys/test-private.pem")) {
      chavePrivadaPem = recurso.readAllBytes();
    }
    String chavePrivadaBase64 =
        new String(chavePrivadaPem, StandardCharsets.UTF_8)
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    var chavePrivada =
        KeyFactory.getInstance("RSA")
            .generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(chavePrivadaBase64)));

    long agora = Instant.now().getEpochSecond();
    String cabecalho = codificar("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
    String payload =
        codificar(
            "{\"iss\":\"https://quickorder\",\"sub\":\"usuario-teste\",\"iat\":"
                + agora
                + ",\"exp\":"
                + (agora + 60)
                + "}");
    String assinaturaBase = cabecalho + "." + payload;

    Signature assinatura = Signature.getInstance("SHA256withRSA");
    assinatura.initSign(chavePrivada);
    assinatura.update(assinaturaBase.getBytes(StandardCharsets.US_ASCII));
    return assinaturaBase
        + "."
        + Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura.sign());
  }

  private String codificar(String valor) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(valor.getBytes(StandardCharsets.UTF_8));
  }
}
