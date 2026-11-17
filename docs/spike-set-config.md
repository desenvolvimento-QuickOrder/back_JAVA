# Spike: aplicação do tenant com `set_config`

## Abordagem

O `DialetoJpaDoTenant` estende `HibernateJpaDialect` e sobrescreve `beginTransaction`. Após iniciar
a transação Hibernate, obtém a conexão da sessão e executa
`select set_config('app.tenant_id', ?, true)` quando `TenantContext` contém um tenant.

O `JpaVendorAdapter` personalizado fornece esse dialeto ao `JpaTransactionManager`. O terceiro
argumento `true` torna a configuração local à transação, sem persistir no pool.

## Resultado

O `HibernateJpaDialect` funcionou sem fallback para uma sobrescrita de
`JpaTransactionManager.doBegin`. Os testes de integração confirmaram que o hook é executado
depois do início da transação, e a configuração com terceiro argumento `true` não vaza para a
próxima transação.

- `IsolamentoPorTenantIT.deveAplicarTenantEmTransacaoSomenteLeitura` passou; o tenant do JWT foi
  aplicado em uma transação somente leitura.
- `IsolamentoPorTenantIT.deveExecutarTenantEmTransacaoNovaDentroDeOutraTransacao` passou; a
  execução interna usou `PROPAGATION_REQUIRES_NEW` e, ao retornar, a transação externa preservou
  sua identidade e permaneceu sem tenant.
- `IsolamentoPorTenantIT.deveIsolarFindByIdJpqlESqlNativoPorRestaurante` passou; JPA, JPQL,
  SQL nativo e `JdbcTemplate` observaram o mesmo isolamento na transação.
- `VazamentoDeTenantIT.naoDeveHerdarTenantEntreTransacoesNaMesmaConexao` passou com
  `maximumPoolSize=1`; a transação seguinte não encontrou linhas e `app.tenant_id` estava vazio.

O ponto de integração está em `DialetoJpaDoTenant.beginTransaction`, registrado por
`ConfiguracaoJpaDoTenant` no `JpaVendorAdapter` personalizado.
