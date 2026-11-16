package com.quickorder.restaurant.core.shared.tenant;

import java.util.UUID;

final class EscopoDeTenant {

  static final ScopedValue<UUID> TENANT_ID = ScopedValue.newInstance();

  private EscopoDeTenant() {}
}
