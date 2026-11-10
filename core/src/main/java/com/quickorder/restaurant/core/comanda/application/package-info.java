/**
 * Casos de uso de comanda: abrir, fechar, transferir mesa, criar pedido (idempotente por {@code
 * Idempotency-Key}), transições da copa e do garçom, cancelamento e fila da copa.
 *
 * <p>Cada transição grava histórico e evento na outbox na mesma transação.
 */
@org.springframework.modulith.NamedInterface("application")
package com.quickorder.restaurant.core.comanda.application;
