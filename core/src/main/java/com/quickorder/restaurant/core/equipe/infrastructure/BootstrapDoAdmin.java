package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeRestaurantes;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapDoAdmin implements ApplicationRunner {

  private final String slug;
  private final String login;
  private final String senha;
  private final RepositorioDeRestaurantes restaurantes;
  private final RepositorioDeUsuarios usuarios;
  private final HashDeSenha hashDeSenha;
  private final ExecucaoNoTenant execucaoNoTenant;

  public BootstrapDoAdmin(
      @Value("${quickorder.bootstrap.slug:}") String slug,
      @Value("${quickorder.bootstrap.admin-login:}") String login,
      @Value("${quickorder.bootstrap.admin-senha:}") String senha,
      RepositorioDeRestaurantes restaurantes,
      RepositorioDeUsuarios usuarios,
      HashDeSenha hashDeSenha,
      ExecucaoNoTenant execucaoNoTenant) {
    this.slug = slug;
    this.login = login;
    this.senha = senha;
    this.restaurantes = restaurantes;
    this.usuarios = usuarios;
    this.hashDeSenha = hashDeSenha;
    this.execucaoNoTenant = execucaoNoTenant;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (slug.isBlank() || login.isBlank() || senha.isBlank()) {
      return;
    }

    restaurantes
        .buscarPorSlug(slug)
        .ifPresent(
            restaurante ->
                execucaoNoTenant.executar(
                    restaurante.id(),
                    () -> {
                      if (!usuarios.possuiAdminAtivo()) {
                        String loginNormalizado = login.trim().toLowerCase(Locale.ROOT);
                        usuarios.salvar(
                            Usuario.novoAdmin(
                                UUID.randomUUID(),
                                restaurante.id(),
                                loginNormalizado,
                                loginNormalizado,
                                hashDeSenha.gerar(senha)));
                      }
                      return null;
                    }));
  }
}
