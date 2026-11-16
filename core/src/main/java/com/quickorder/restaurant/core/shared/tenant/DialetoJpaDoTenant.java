package com.quickorder.restaurant.core.shared.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

public class DialetoJpaDoTenant extends HibernateJpaDialect {

  private static final String SQL_SET_CONFIG = "select set_config('app.tenant_id', ?, true)";

  private final TenantContext tenantContext;

  public DialetoJpaDoTenant(TenantContext tenantContext) {
    this.tenantContext = tenantContext;
  }

  @Override
  public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
      throws PersistenceException, SQLException, TransactionException {
    Object transacao = super.beginTransaction(entityManager, definition);
    Optional<UUID> tenantId = tenantContext.tenantAtual();
    if (tenantId.isPresent()) {
      entityManager
          .unwrap(Session.class)
          .doWork(
              connection -> {
                try (PreparedStatement statement = connection.prepareStatement(SQL_SET_CONFIG)) {
                  statement.setString(1, tenantId.get().toString());
                  statement.execute();
                }
              });
    }
    return transacao;
  }
}
