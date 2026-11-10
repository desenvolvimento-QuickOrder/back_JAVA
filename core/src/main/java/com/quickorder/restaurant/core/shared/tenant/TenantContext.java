package com.quickorder.restaurant.core.shared.tenant;

import java.util.Optional;
import java.util.UUID;

public interface TenantContext {

  Optional<UUID> tenantAtual();
}
