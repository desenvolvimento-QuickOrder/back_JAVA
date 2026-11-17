package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.Dispositivo;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeDispositivos;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.shared.error.RecursoNaoEncontrado;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RevogarDispositivo {

  private final RepositorioDeDispositivos dispositivos;
  private final RepositorioDeSessoesDeRefresh sessoes;
  private final Relogio relogio;
  private final ExecucaoNoTenant execucaoNoTenant;

  public RevogarDispositivo(
      RepositorioDeDispositivos dispositivos,
      RepositorioDeSessoesDeRefresh sessoes,
      Relogio relogio,
      ExecucaoNoTenant execucaoNoTenant) {
    this.dispositivos = dispositivos;
    this.sessoes = sessoes;
    this.relogio = relogio;
    this.execucaoNoTenant = execucaoNoTenant;
  }

  public Dispositivo executar(UUID restauranteId, UUID dispositivoId) {
    return execucaoNoTenant.executar(
        restauranteId,
        () -> {
          Dispositivo dispositivo =
              dispositivos
                  .buscarPorId(dispositivoId)
                  .orElseThrow(() -> new RecursoNaoEncontrado("Dispositivo não encontrado."));
          Dispositivo revogado = dispositivo.revogar(relogio.agora());
          dispositivos.salvar(revogado);
          sessoes.revogarTodasDoDispositivo(dispositivoId, relogio.agora());
          return revogado;
        });
  }
}
