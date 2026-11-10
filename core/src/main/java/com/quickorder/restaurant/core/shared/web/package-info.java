/**
 * Infraestrutura web transversal.
 *
 * <ul>
 *   <li>Filtro de {@code X-Correlation-Id} (recebido ou gerado), propagado no MDC e nas respostas.
 *   <li>{@code ClientIpResolver}: IP do cliente pelo N-ésimo item da direita do {@code
 *       X-Forwarded-For} (proxy do Render), usado no rate limit.
 *   <li>Configuração de CORS restrita às origens do front.
 *   <li>Rate limit com Bucket4j (por IP e por conta no login).
 * </ul>
 */
package com.quickorder.restaurant.core.shared.web;
