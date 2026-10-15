/**
 * Tratamento global de erros no padrão RFC 9457 ({@code application/problem+json}).
 *
 * <p>Handler único ({@code @RestControllerAdvice}) que converte exceções de domínio,
 * validação e segurança em {@code ProblemDetail} com o campo {@code code} estável
 * (por exemplo {@code VALIDACAO_INVALIDA}, {@code CONFLITO_DE_VERSAO},
 * {@code PEDIDO_TRANSICAO_INVALIDA}) e o {@code correlationId} da requisição.
 */
package com.quickorder.restaurant.core.shared.error;
