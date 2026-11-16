package com.quickorder.restaurant.core.shared.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "isolamento_teste")
public class RegistroDeIsolamentoTeste {

  @Id private UUID id;

  @Column(name = "restaurante_id", nullable = false)
  private UUID restauranteId;

  @Column(columnDefinition = "text")
  private String valor;

  protected RegistroDeIsolamentoTeste() {}

  public UUID getId() {
    return id;
  }

  public UUID getRestauranteId() {
    return restauranteId;
  }

  public String getValor() {
    return valor;
  }
}
