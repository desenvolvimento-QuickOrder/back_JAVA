package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class UsuarioJpaEntity {

  @Id private UUID id;

  @Column(name = "restaurante_id", nullable = false)
  private UUID restauranteId;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, length = 80)
  private String login;

  @Column(name = "senha_hash", nullable = false, length = 255)
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Papel papel;

  @Column(nullable = false)
  private boolean ativo;

  protected UsuarioJpaEntity() {}

  UsuarioJpaEntity(Usuario usuario) {
    id = usuario.id();
    restauranteId = usuario.restauranteId();
    nome = usuario.nome();
    login = usuario.login();
    senhaHash = usuario.senhaHash();
    papel = usuario.papel();
    ativo = usuario.ativo();
  }

  Usuario paraDominio() {
    return new Usuario(id, restauranteId, nome, login, senhaHash, papel, ativo);
  }
}
