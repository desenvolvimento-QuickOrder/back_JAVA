package com.quickorder.restaurant.testweb;

import com.quickorder.restaurant.core.shared.error.ConflitoDeNegocio;
import com.quickorder.restaurant.core.shared.error.ExcecaoDeNegocio;
import com.quickorder.restaurant.core.shared.error.RecursoNaoEncontrado;
import com.quickorder.restaurant.core.shared.tenant.RegistroDeIsolamentoTeste;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teste")
public class ControladorDeErrosTeste {

  private final JdbcTemplate jdbcTemplate;

  public ControladorDeErrosTeste(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/erros/negocio")
  void falharRegraDeNegocio() {
    throw new ExcecaoDeNegocio("REGRA_TESTE", "A regra de teste foi violada.");
  }

  @GetMapping("/erros/nao-encontrado")
  void falharRecursoNaoEncontrado() {
    throw new RecursoNaoEncontrado("Recurso de teste não encontrado.");
  }

  @GetMapping("/erros/conflito")
  void falharConflitoDeNegocio() {
    throw new ConflitoDeNegocio("CONFLITO_TESTE", "Conflito de teste.");
  }

  @GetMapping("/erros/conflito-de-versao")
  void falharConflitoDeVersao() {
    throw new ObjectOptimisticLockingFailureException(
        RegistroDeIsolamentoTeste.class, UUID.randomUUID());
  }

  @GetMapping("/erros/constraint-desconhecida")
  void falharConstraintSemTraducao() {
    throw new DataIntegrityViolationException("constraint não registrada");
  }

  @GetMapping("/erros/inesperado")
  void falharDeFormaInesperada() {
    throw new IllegalStateException("detalhe interno confidencial");
  }

  @GetMapping("/erros/uuid/{id}")
  String validarUuid(@PathVariable UUID id) {
    return id.toString();
  }

  @PostMapping(path = "/erros/json", consumes = MediaType.APPLICATION_JSON_VALUE)
  String validarJson(@Valid @RequestBody EntradaDeTeste entrada) {
    return entrada.nome();
  }

  @PostMapping("/erros/duplicidade/{id}/{restauranteId}")
  @Transactional
  public void causarDuplicidade(@PathVariable UUID id, @PathVariable UUID restauranteId) {
    jdbcTemplate.update(
        "insert into isolamento_teste (id, restaurante_id, valor) values (?, ?, ?)",
        id,
        restauranteId,
        "duplicado");
  }

  @GetMapping("/seguranca/protegido")
  String protegido() {
    return "protegido";
  }

  public record EntradaDeTeste(@NotBlank String nome) {}
}
