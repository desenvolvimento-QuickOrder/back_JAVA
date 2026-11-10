/**
 * Kernel compartilhado entre os módulos de negócio.
 *
 * <p>Contém apenas o que é transversal: contexto de tenant, tratamento de erros, envelope de
 * eventos e outbox, infraestrutura web e segurança. Não depende de nenhum módulo de negócio; os
 * módulos dependem dele.
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.quickorder.restaurant.core.shared;
