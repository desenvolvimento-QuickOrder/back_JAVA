package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.Dispositivo;
import com.quickorder.restaurant.core.equipe.domain.PaginaDeDispositivos;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeDispositivos;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeDispositivosJpa implements RepositorioDeDispositivos {

  private final RepositorioJpaDeDispositivos repositorio;

  RepositorioDeDispositivosJpa(RepositorioJpaDeDispositivos repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public Optional<Dispositivo> buscarPorId(UUID id) {
    return repositorio.findById(id).map(DispositivoJpaEntity::paraDominio);
  }

  @Override
  public Optional<Dispositivo> buscarPorHashDoToken(String tokenHash) {
    return repositorio.findByTokenHash(tokenHash).map(DispositivoJpaEntity::paraDominio);
  }

  @Override
  public PaginaDeDispositivos listar(int pagina, int tamanho) {
    return new PaginaDeDispositivos(
        repositorio.listar(PageRequest.of(pagina, tamanho)).stream()
            .map(DispositivoJpaEntity::paraDominio)
            .toList(),
        pagina,
        tamanho,
        repositorio.contar());
  }

  @Override
  public void salvar(Dispositivo dispositivo) {
    repositorio.saveAndFlush(new DispositivoJpaEntity(dispositivo));
  }
}
