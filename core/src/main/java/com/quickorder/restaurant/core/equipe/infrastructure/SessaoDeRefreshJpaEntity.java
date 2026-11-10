package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.SessaoDeRefresh;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "refresh_token")
public class SessaoDeRefreshJpaEntity {

  @Id private UUID id;

  @Column(name = "restaurante_id", nullable = false)
  private UUID restauranteId;

  @Column(name = "usuario_id", nullable = false)
  private UUID usuarioId;

  @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "char(64)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String tokenHash;

  @Column(name = "familia_id", nullable = false)
  private UUID familiaId;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  @Column(name = "expira_em", nullable = false)
  private Instant expiraEm;

  @Column(name = "familia_expira_em", nullable = false)
  private Instant familiaExpiraEm;

  @Column(name = "usado_em")
  private Instant usadoEm;

  @Column(name = "revogado_em")
  private Instant revogadoEm;

  protected SessaoDeRefreshJpaEntity() {}

  SessaoDeRefreshJpaEntity(SessaoDeRefresh sessao) {
    id = sessao.id();
    restauranteId = sessao.restauranteId();
    usuarioId = sessao.usuarioId();
    tokenHash = sessao.tokenHash();
    familiaId = sessao.familiaId();
    criadoEm = sessao.criadoEm();
    expiraEm = sessao.expiraEm();
    familiaExpiraEm = sessao.familiaExpiraEm();
    usadoEm = sessao.usadoEm();
    revogadoEm = sessao.revogadoEm();
  }

  SessaoDeRefresh paraDominio() {
    return new SessaoDeRefresh(
        id,
        restauranteId,
        usuarioId,
        tokenHash,
        familiaId,
        criadoEm,
        expiraEm,
        familiaExpiraEm,
        usadoEm,
        revogadoEm);
  }
}
