package com.quickorder.restaurant.core.equipe.web;

import com.quickorder.restaurant.core.equipe.application.DispositivoCriado;
import com.quickorder.restaurant.core.equipe.application.ListarDispositivos;
import com.quickorder.restaurant.core.equipe.application.RegistrarDispositivo;
import com.quickorder.restaurant.core.equipe.application.RevogarDispositivo;
import com.quickorder.restaurant.core.equipe.domain.Dispositivo;
import com.quickorder.restaurant.core.equipe.domain.PaginaDeDispositivos;
import com.quickorder.restaurant.core.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/dispositivos")
public class DispositivosController {

  private final RegistrarDispositivo registrarDispositivo;
  private final ListarDispositivos listarDispositivos;
  private final RevogarDispositivo revogarDispositivo;
  private final TenantContext tenantContext;

  public DispositivosController(
      RegistrarDispositivo registrarDispositivo,
      ListarDispositivos listarDispositivos,
      RevogarDispositivo revogarDispositivo,
      TenantContext tenantContext) {
    this.registrarDispositivo = registrarDispositivo;
    this.listarDispositivos = listarDispositivos;
    this.revogarDispositivo = revogarDispositivo;
    this.tenantContext = tenantContext;
  }

  @PostMapping
  public ResponseEntity<RespostaDeRegistroDeDispositivo> registrar(
      @Valid @RequestBody RequisicaoDeRegistroDeDispositivo requisicao) {
    UUID restauranteId = tenantContext.tenantAtual().orElseThrow();
    UUID usuarioId = usuarioAutenticado();
    DispositivoCriado criado =
        registrarDispositivo.executar(restauranteId, usuarioId, requisicao.nome());
    return ResponseEntity.created(URI.create("/api/v1/dispositivos/" + criado.dispositivo().id()))
        .body(
            new RespostaDeRegistroDeDispositivo(
                criado.dispositivo().id(), criado.dispositivo().nome(), criado.deviceToken()));
  }

  @GetMapping
  public ResponseEntity<RespostaDePaginaDeDispositivos> listar(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    PaginaDeDispositivos pagina =
        listarDispositivos.executar(tenantContext.tenantAtual().orElseThrow(), page, size);
    return ResponseEntity.ok(
        new RespostaDePaginaDeDispositivos(
            pagina.items().stream().map(DispositivosController::resposta).toList(),
            pagina.page(),
            pagina.size(),
            pagina.total()));
  }

  @PostMapping("/{dispositivoId}/revogar")
  public ResponseEntity<RespostaDeDispositivo> revogar(@PathVariable UUID dispositivoId) {
    Dispositivo dispositivo =
        revogarDispositivo.executar(tenantContext.tenantAtual().orElseThrow(), dispositivoId);
    return ResponseEntity.ok(resposta(dispositivo));
  }

  private static UUID usuarioAutenticado() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    return UUID.fromString(((JwtAuthenticationToken) autenticacao).getToken().getSubject());
  }

  private static RespostaDeDispositivo resposta(Dispositivo dispositivo) {
    return new RespostaDeDispositivo(
        dispositivo.id(),
        dispositivo.nome(),
        dispositivo.criadoEm(),
        dispositivo.ultimoUsoEm(),
        dispositivo.ativo());
  }

  public record RequisicaoDeRegistroDeDispositivo(@NotBlank @Size(max = 80) String nome) {}

  public record RespostaDeRegistroDeDispositivo(UUID id, String nome, String deviceToken) {}

  public record RespostaDeDispositivo(
      UUID id, String nome, Instant criadoEm, Instant ultimoUsoEm, boolean ativo) {}

  public record RespostaDePaginaDeDispositivos(
      List<RespostaDeDispositivo> items, int page, int size, long total) {}
}
