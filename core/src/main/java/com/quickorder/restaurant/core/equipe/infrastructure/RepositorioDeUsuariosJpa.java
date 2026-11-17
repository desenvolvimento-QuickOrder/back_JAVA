package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.PaginaDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeUsuariosJpa implements RepositorioDeUsuarios {

  private final RepositorioJpaDeUsuarios repositorio;

  RepositorioDeUsuariosJpa(RepositorioJpaDeUsuarios repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public Optional<Usuario> buscarPorLogin(String login) {
    return repositorio
        .findByLoginAndPapelAndAtivoTrue(login, Papel.ADMIN)
        .map(UsuarioJpaEntity::paraDominio);
  }

  @Override
  public Optional<Usuario> buscarPorCodigo(String codigo) {
    return repositorio.findByCodigo(codigo).map(UsuarioJpaEntity::paraDominio);
  }

  @Override
  public Optional<Usuario> buscarPorId(UUID id) {
    return repositorio.findById(id).map(UsuarioJpaEntity::paraDominio);
  }

  @Override
  public boolean possuiAdminAtivo() {
    return repositorio.existsByPapelAndAtivoTrue(Papel.ADMIN);
  }

  @Override
  public boolean existeLogin(String login) {
    return repositorio.existsByLogin(login);
  }

  @Override
  public List<Usuario> bloquearAdminsAtivos() {
    return repositorio.bloquearAdminsAtivos().stream().map(UsuarioJpaEntity::paraDominio).toList();
  }

  @Override
  public PaginaDeUsuarios listar(Papel papel, Boolean ativo, int pagina, int tamanho) {
    var ordenacao = Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("id"));
    List<Usuario> usuarios =
        repositorio.listar(papel, ativo, PageRequest.of(pagina, tamanho, ordenacao)).stream()
            .map(UsuarioJpaEntity::paraDominio)
            .toList();
    return new PaginaDeUsuarios(pagina, tamanho, repositorio.contar(papel, ativo), usuarios);
  }

  @Override
  public Usuario salvar(Usuario usuario) {
    return repositorio.saveAndFlush(new UsuarioJpaEntity(usuario)).paraDominio();
  }
}
