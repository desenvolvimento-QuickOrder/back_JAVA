package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.Dispositivo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "dispositivo")
public class DispositivoJpaEntity {

  @Id private UUID id;

  @Column(name = "restaurante_id", nullable = false)
  private UUID restauranteId;

  @Column(nullable = false, length = 80)
  private String nome;

  @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "char(64)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String tokenHash;

  @Column(name = "criado_por", nullable = false)
  private UUID criadoPor;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  @Column(name = "ultimo_uso_em")
  private Instant ultimoUsoEm;

  @Column(name = "revogado_em")
  private Instant revogadoEm;

  protected DispositivoJpaEntity() {}

  DispositivoJpaEntity(Dispositivo dispositivo) {
    id = dispositivo.id();
    restauranteId = dispositivo.restauranteId();
    nome = dispositivo.nome();
    tokenHash = dispositivo.tokenHash();
    criadoPor = dispositivo.criadoPor();
    criadoEm = dispositivo.criadoEm();
    ultimoUsoEm = dispositivo.ultimoUsoEm();
    revogadoEm = dispositivo.revogadoEm();
  }

  Dispositivo paraDominio() {
    return new Dispositivo(
        id, restauranteId, nome, tokenHash, criadoPor, criadoEm, ultimoUsoEm, revogadoEm);
  }
}
