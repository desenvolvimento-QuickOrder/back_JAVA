package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.PaginaDeDispositivos;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeDispositivos;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListarDispositivos {

  private final RepositorioDeDispositivos dispositivos;
  private final ExecucaoNoTenant execucaoNoTenant;

  public ListarDispositivos(
      RepositorioDeDispositivos dispositivos, ExecucaoNoTenant execucaoNoTenant) {
    this.dispositivos = dispositivos;
    this.execucaoNoTenant = execucaoNoTenant;
  }

  public PaginaDeDispositivos executar(UUID restauranteId, int pagina, int tamanho) {
    return execucaoNoTenant.executar(restauranteId, () -> dispositivos.listar(pagina, tamanho));
  }
}
