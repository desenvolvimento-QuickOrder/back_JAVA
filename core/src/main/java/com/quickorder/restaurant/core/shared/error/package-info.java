/**
 * Tratamento global de erros no padrão RFC 9457 ({@code application/problem+json}).
 *
 * <p>Handler global ({@code @RestControllerAdvice}) que converte falhas de validação e segurança
 * em {@code ProblemDetail} com o campo {@code code} estável (por exemplo {@code
 * VALIDACAO_INVALIDA}, {@code CONFLITO_DE_VERSAO}, {@code PEDIDO_TRANSICAO_INVALIDA}) e o {@code
 * correlationId} da requisição. Exceções de domínio são tratadas pelos módulos que as definem.
 */
package com.quickorder.restaurant.core.shared.error;
