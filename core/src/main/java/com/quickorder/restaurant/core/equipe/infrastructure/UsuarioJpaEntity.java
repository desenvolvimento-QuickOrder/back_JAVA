package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuario")
public class UsuarioJpaEntity {

  @Id private UUID id;

  @Column(name = "restaurante_id", nullable = false)
  private UUID restauranteId;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(length = 80)
  private String login;

  @Column(name = "senha_hash", length = 255)
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Papel papel;

  @Column(nullable = false)
  private boolean ativo;

  @Column(name = "codigo", length = 4, columnDefinition = "char(4)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String codigo;

  @Column(name = "pin_hash", length = 255)
  private String pinHash;

  @Column(name = "deve_trocar_pin", nullable = false)
  private boolean deveTrocarPin;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected UsuarioJpaEntity() {}

  UsuarioJpaEntity(Usuario usuario) {
    id = usuario.id();
    restauranteId = usuario.restauranteId();
    nome = usuario.nome();
    login = usuario.login();
    senhaHash = usuario.senhaHash();
    papel = usuario.papel();
    ativo = usuario.ativo();
    codigo = usuario.codigo();
    pinHash = usuario.pinHash();
    deveTrocarPin = usuario.deveTrocarPin();
    version = usuario.versao();
  }

  Usuario paraDominio() {
    return new Usuario(
        id,
        restauranteId,
        nome,
        login,
        senhaHash,
        papel,
        ativo,
        codigo,
        pinHash,
        deveTrocarPin,
        version);
  }

  @jakarta.persistence.PrePersist
  @jakarta.persistence.PreUpdate
  void atualizarInstanteDeAtualizacao() {
    atualizadoEm = Instant.now();
  }
}
