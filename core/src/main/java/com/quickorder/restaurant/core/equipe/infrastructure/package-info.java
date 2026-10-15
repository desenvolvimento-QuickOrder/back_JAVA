/**
 * Adaptadores de saída de equipe: repositórios JPA de {@code usuario} e
 * {@code refresh_token}, assinatura RS256 e publicação do JWKS.
 *
 * <p>{@code refresh_token} é tabela técnica sem RLS (busca por hash antes de se conhecer o
 * tenant).
 */
package com.quickorder.restaurant.core.equipe.infrastructure;
