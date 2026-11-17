package com.quickorder.restaurant.core.mesa.web;

import com.quickorder.restaurant.core.mesa.application.AtualizarMesaUseCase;
import com.quickorder.restaurant.core.mesa.application.CriarMesaUseCase;
import com.quickorder.restaurant.core.mesa.application.ListarMesasUseCase;
import com.quickorder.restaurant.core.mesa.application.MesaPage;
import com.quickorder.restaurant.core.mesa.application.ObterMesaUseCase;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mesas")
public class MesaController {

  private final CriarMesaUseCase criarMesaUseCase;
  private final ListarMesasUseCase listarMesasUseCase;
  private final ObterMesaUseCase obterMesaUseCase;
  private final AtualizarMesaUseCase atualizarMesaUseCase;
  private final MesaMapper mesaMapper;

  public MesaController(
      CriarMesaUseCase criarMesaUseCase,
      ListarMesasUseCase listarMesasUseCase,
      ObterMesaUseCase obterMesaUseCase,
      AtualizarMesaUseCase atualizarMesaUseCase,
      MesaMapper mesaMapper) {
    this.criarMesaUseCase = criarMesaUseCase;
    this.listarMesasUseCase = listarMesasUseCase;
    this.obterMesaUseCase = obterMesaUseCase;
    this.atualizarMesaUseCase = atualizarMesaUseCase;
    this.mesaMapper = mesaMapper;
  }

  @PostMapping
  public ResponseEntity<MesaResponse> criar(@Valid @RequestBody MesaCreateRequest request) {
    var mesa = criarMesaUseCase.execute(request.numero(), request.capacidade());
    var response = mesaMapper.toResponse(mesa);
    return ResponseEntity.created(URI.create("/api/v1/mesas/" + mesa.id())).body(response);
  }

  @GetMapping
  public MesaPageResponse<MesaResponse> listar(
      @RequestParam(required = false) MesaStatus status,
      @RequestParam(required = false) Boolean ativa,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    MesaPage<com.quickorder.restaurant.core.mesa.domain.Mesa> pageResult =
        listarMesasUseCase.execute(page, size, status, ativa);
    var items = pageResult.items().stream().map(mesaMapper::toResponse).toList();
    return new MesaPageResponse<>(
        pageResult.page(),
        pageResult.size(),
        pageResult.totalElements(),
        pageResult.totalPages(),
        items);
  }

  @GetMapping("/{id}")
  public MesaResponse obter(@PathVariable UUID id) {
    return mesaMapper.toResponse(obterMesaUseCase.execute(id));
  }

  @PatchMapping("/{id}")
  public MesaResponse atualizar(
      @PathVariable UUID id, @Valid @RequestBody MesaUpdateRequest request) {
    if (request.vazio()
        || (request.numero() != null
            && request.numero().isPresent()
            && request.numero().get() == null)
        || (request.capacidade() != null
            && request.capacidade().isPresent()
            && request.capacidade().get() == null)
        || (request.ativa() != null
            && request.ativa().isPresent()
            && request.ativa().get() == null)) {
      throw new IllegalArgumentException("ATUALIZACAO_INVALIDA");
    }
    var mesa =
        atualizarMesaUseCase.execute(
            id,
            request.numero() == null || !request.numero().isPresent()
                ? null
                : request.numero().get(),
            request.capacidade() == null || !request.capacidade().isPresent()
                ? null
                : request.capacidade().get(),
            request.ativa() == null || !request.ativa().isPresent() ? null : request.ativa().get());
    return mesaMapper.toResponse(mesa);
  }
}
