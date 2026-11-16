package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.Dispositivo;
import com.quickorder.restaurant.core.equipe.domain.GeradorDeTokenOpaco;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeDispositivos;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RegistrarDispositivo {

  private final RepositorioDeDispositivos dispositivos;
  private final GeradorDeTokenOpaco geradorDeToken;
  private final Relogio relogio;
  private final ExecucaoNoTenant execucaoNoTenant;

  public RegistrarDispositivo(
      RepositorioDeDispositivos dispositivos,
      GeradorDeTokenOpaco geradorDeToken,
      Relogio relogio,
      ExecucaoNoTenant execucaoNoTenant) {
    this.dispositivos = dispositivos;
    this.geradorDeToken = geradorDeToken;
    this.relogio = relogio;
    this.execucaoNoTenant = execucaoNoTenant;
  }

  public DispositivoCriado executar(UUID restauranteId, UUID usuarioId, String nome) {
    String token = geradorDeToken.gerar();
    Dispositivo dispositivo =
        new Dispositivo(
            UUID.randomUUID(),
            restauranteId,
            nome.trim(),
            geradorDeToken.hash(token),
            usuarioId,
            relogio.agora(),
            null,
            null);
    execucaoNoTenant.executar(
        restauranteId,
        () -> {
          dispositivos.salvar(dispositivo);
          return null;
        });
    return new DispositivoCriado(dispositivo, token);
  }
}
