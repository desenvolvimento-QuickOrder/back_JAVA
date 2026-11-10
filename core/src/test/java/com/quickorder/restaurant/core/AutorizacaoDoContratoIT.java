package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@SpringBootTest
@AutoConfigureMockMvc
class AutorizacaoDoContratoIT extends IntegracaoBaseIT {

  private static final List<String> PAPEIS = List.of("ADMIN", "GARCOM", "COPA");
  private static final Set<String> METODOS_HTTP =
      Set.of("get", "post", "put", "patch", "delete", "options", "head", "trace");

  @Autowired private MockMvc mockMvc;

  @Test
  void deveAplicarOsPapeisDeCadaOperacaoDoContrato() throws Exception {
    Map<String, Object> contrato = carregarYaml(arquivoOpenApi());
    String prefixo = prefixoDoServidor(contrato);
    Map<String, Object> caminhos = mapa(contrato.get("paths"));

    for (var caminho : caminhos.entrySet()) {
      Map<String, Object> referencia = mapa(caminho.getValue());
      Map<String, Object> operacoes =
          resolverReferencia(arquivoOpenApi(), (String) referencia.get("$ref"));
      for (var entrada : operacoes.entrySet()) {
        String metodo = entrada.getKey().toLowerCase();
        if (!METODOS_HTTP.contains(metodo)) {
          continue;
        }
        Map<String, Object> operacao = mapa(entrada.getValue());
        Set<String> permitidos = papeisPermitidos(operacao);
        String caminhoRequisicao = prefixo + substituirParametros((String) caminho.getKey());
        for (String papel : PAPEIS) {
          MockHttpServletRequestBuilder requisicao =
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                      HttpMethod.valueOf(metodo.toUpperCase()), caminhoRequisicao)
                  .with(
                      jwt()
                          .jwt(
                              token ->
                                  token
                                      .claim("roles", List.of(papel))
                                      .claim("tenant_id", "00000000-0000-0000-0000-000000000001"))
                          .authorities(new SimpleGrantedAuthority("ROLE_" + papel)));
          if (Set.of("post", "put", "patch").contains(metodo)) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content("{}");
          }

          int status = mockMvc.perform(requisicao).andReturn().getResponse().getStatus();
          String contexto = metodo.toUpperCase() + " " + caminho.getKey() + " para " + papel;
          if (permitidos.contains(papel)) {
            assertThat(status).as(contexto).isNotEqualTo(403);
          } else {
            assertThat(status).as(contexto).isEqualTo(403);
          }
        }
      }
    }
  }

  private static Set<String> papeisPermitidos(Map<String, Object> operacao) {
    Object papeis = operacao.get("x-roles");
    if (papeis instanceof List<?> lista) {
      return lista.stream().map(String.class::cast).collect(java.util.stream.Collectors.toSet());
    }
    Object seguranca = operacao.get("security");
    if (seguranca instanceof List<?> lista && lista.isEmpty()) {
      return Set.copyOf(PAPEIS);
    }
    throw new AssertionError("Operação sem x-roles ou security: [] no contrato: " + operacao);
  }

  private static String prefixoDoServidor(Map<String, Object> contrato) {
    List<?> servidores = (List<?>) contrato.get("servers");
    return (String) mapa(servidores.getFirst()).get("url");
  }

  private static String substituirParametros(String caminho) {
    return caminho.replaceAll("\\{[^}]+}", "00000000-0000-0000-0000-000000000001");
  }

  private static Map<String, Object> resolverReferencia(Path openApi, String referencia)
      throws IOException {
    String[] partes = referencia.split("#", 2);
    Path documento = openApi.getParent().resolve(partes[0]).normalize();
    Object valor = carregarYaml(documento);
    if (partes.length == 2 && !partes[1].isBlank()) {
      for (String segmento : partes[1].substring(1).split("/")) {
        valor = mapa(valor).get(segmento.replace("~1", "/").replace("~0", "~"));
      }
    }
    return mapa(valor);
  }

  private static Map<String, Object> carregarYaml(Path arquivo) throws IOException {
    try (var entrada = Files.newInputStream(arquivo)) {
      Object yaml = new Yaml(new SafeConstructor(new LoaderOptions())).load(entrada);
      return mapa(yaml);
    }
  }

  private static Path arquivoOpenApi() {
    Path diretorio = Path.of("").toAbsolutePath();
    while (diretorio != null) {
      Path candidato = diretorio.resolve("api/openapi.yaml");
      if (Files.isRegularFile(candidato)) {
        return candidato;
      }
      diretorio = diretorio.getParent();
    }
    throw new IllegalStateException("Não foi possível localizar api/openapi.yaml.");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> mapa(Object valor) {
    if (!(valor instanceof Map<?, ?>)) {
      throw new AssertionError("Era esperado um mapa YAML, mas recebeu: " + valor);
    }
    return (Map<String, Object>) valor;
  }
}
