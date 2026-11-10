package com.quickorder.restaurant.core.shared.tenant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class ContextoDeTenant implements TenantContext {

  @Override
  public Optional<UUID> tenantAtual() {
    if (EscopoDeTenant.TENANT_ID.isBound()) {
      return Optional.of(EscopoDeTenant.TENANT_ID.get());
    }

    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (!(autenticacao instanceof JwtAuthenticationToken token)) {
      return Optional.empty();
    }

    String tenantId = token.getToken().getClaimAsString("tenant_id");
    if (tenantId == null || tenantId.isBlank()) {
      return Optional.empty();
    }

    try {
      return Optional.of(UUID.fromString(tenantId));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
