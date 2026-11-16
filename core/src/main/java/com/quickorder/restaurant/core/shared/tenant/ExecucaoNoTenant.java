package com.quickorder.restaurant.core.shared.tenant;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ExecucaoNoTenant {

  private final TransactionTemplate transacao;

  public ExecucaoNoTenant(PlatformTransactionManager transactionManager) {
    transacao = new TransactionTemplate(transactionManager);
    transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public <T> T executar(UUID tenantId, Supplier<T> operacao) {
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(operacao);
    try {
      return ScopedValue.where(EscopoDeTenant.TENANT_ID, tenantId)
          .call(() -> transacao.execute(status -> operacao.get()));
    } catch (RuntimeException | Error exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao executar a operação no tenant.", exception);
    }
  }
}
