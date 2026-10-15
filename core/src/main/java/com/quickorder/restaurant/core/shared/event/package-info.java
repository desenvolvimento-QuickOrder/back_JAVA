/**
 * Eventos de domínio, envelope padrão e outbox transacional.
 *
 * <ul>
 *   <li>Envelope: {@code eventId}, {@code tipo}, {@code schemaVersion}, {@code tenantId} e
 *       {@code payload}. Eventos de pedido levam {@code pedidoVersion}.</li>
 *   <li>O evento é gravado na tabela {@code outbox} na mesma transação da mudança de estado.</li>
 *   <li>Um publicador único lê {@code outbox} onde {@code publicado_em IS NULL} e publica em
 *       Redis Streams ({@code events:pedido}, {@code events:mesa}).</li>
 *   <li>Eventos são dicas: a verdade é o REST.</li>
 * </ul>
 *
 * <p>Catálogo e formato documentados em {@code docs/events}.
 */
package com.quickorder.restaurant.core.shared.event;
