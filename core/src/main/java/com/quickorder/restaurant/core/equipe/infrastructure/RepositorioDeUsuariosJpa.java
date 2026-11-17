package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionarioDuplicado;
import com.quickorder.restaurant.core.equipe.domain.PaginaDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
  public List<String> codigosUsados() {
    return repositorio.codigosUsados().stream().map(String::trim).toList();
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
    try {
      return repositorio.saveAndFlush(new UsuarioJpaEntity(usuario)).paraDominio();
    } catch (DataIntegrityViolationException excecao) {
      if (nomeDaConstraint(excecao).filter("uq_usuario_codigo"::equals).isPresent()) {
        throw new CodigoFuncionarioDuplicado();
      }
      throw excecao;
    }
  }

  private static Optional<String> nomeDaConstraint(Throwable causaRaiz) {
    var causas = new ArrayDeque<Throwable>();
    Set<Throwable> visitadas = new HashSet<>();
    causas.add(causaRaiz);
    while (!causas.isEmpty()) {
      Throwable causa = causas.removeFirst();
      if (!visitadas.add(causa)) {
        continue;
      }
      if (causa instanceof ConstraintViolationException violacao
          && violacao.getConstraintName() != null) {
        return Optional.of(violacao.getConstraintName());
      }
      if (causa.getCause() != null) {
        causas.addLast(causa.getCause());
      }
      for (Throwable suprimida : causa.getSuppressed()) {
        causas.addLast(suprimida);
      }
    }
    return Optional.empty();
  }
}
