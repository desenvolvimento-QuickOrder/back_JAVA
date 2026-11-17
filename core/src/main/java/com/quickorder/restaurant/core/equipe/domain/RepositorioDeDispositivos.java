package com.quickorder.restaurant.core.equipe.domain;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeDispositivos {

  Optional<Dispositivo> buscarPorId(UUID id);

  Optional<Dispositivo> buscarPorHashDoToken(String tokenHash);

  PaginaDeDispositivos listar(int pagina, int tamanho);

  void salvar(Dispositivo dispositivo);
}
