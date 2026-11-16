package com.quickorder.restaurant.core.mesa.infrastructure;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mesa")
public class MesaEntity {

  public static final UUID DEFAULT_RESTAURANTE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Id
  @Column(name = "id", nullable = false, columnDefinition = "uuid")
  private UUID id;

  @Column(name = "restaurante_id", nullable = false)
  private UUID restauranteId;

  @Column(name = "numero", nullable = false, length = 20)
  private String numero;

  @Column(name = "capacidade", nullable = false)
  private Integer capacidade;

  @Column(name = "ativa", nullable = false)
  private Boolean ativa;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  @Transient private MesaStatus status = MesaStatus.LIVRE;

  protected MesaEntity() {}

  public MesaEntity(UUID id, UUID restauranteId, String numero, Integer capacidade, Boolean ativa) {
    this.id = id;
    this.restauranteId = restauranteId;
    this.numero = numero;
    this.capacidade = capacidade;
    this.ativa = ativa;
  }

  public static MesaEntity fromDomain(Mesa mesa) {
    return new MesaEntity(
        mesa.id(), DEFAULT_RESTAURANTE_ID, mesa.numero(), mesa.capacidade(), mesa.ativa());
  }

  public Mesa toDomain() {
    return new Mesa(id, numero, capacidade, ativa, status == null ? MesaStatus.LIVRE : status);
  }

  @PrePersist
  public void prePersist() {
    if (restauranteId == null) {
      restauranteId = DEFAULT_RESTAURANTE_ID;
    }
    if (criadoEm == null) {
      criadoEm = Instant.now();
    }
    if (atualizadoEm == null) {
      atualizadoEm = Instant.now();
    }
  }

  @PreUpdate
  public void preUpdate() {
    atualizadoEm = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getRestauranteId() {
    return restauranteId;
  }

  public String getNumero() {
    return numero;
  }

  public Integer getCapacidade() {
    return capacidade;
  }

  public Boolean getAtiva() {
    return ativa;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  public MesaStatus getStatus() {
    return status;
  }

  public void setStatus(MesaStatus status) {
    this.status = status;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setRestauranteId(UUID restauranteId) {
    this.restauranteId = restauranteId;
  }

  public void setNumero(String numero) {
    this.numero = numero;
  }

  public void setCapacidade(Integer capacidade) {
    this.capacidade = capacidade;
  }

  public void setAtiva(Boolean ativa) {
    this.ativa = ativa;
  }

  public void setCriadoEm(Instant criadoEm) {
    this.criadoEm = criadoEm;
  }

  public void setAtualizadoEm(Instant atualizadoEm) {
    this.atualizadoEm = atualizadoEm;
  }
}
