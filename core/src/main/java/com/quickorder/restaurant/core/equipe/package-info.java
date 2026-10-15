/**
 * Módulo equipe: autenticação, usuários e papéis ({@code ADMIN}, {@code GARCOM}, {@code COPA}).
 *
 * <ul>
 *   <li>Login por restaurante (slug) e login; senha com hash forte.</li>
 *   <li>Emissão do access token JWT RS256 (claims {@code sub}, {@code roles},
 *       {@code tenant_id}, cerca de 15 minutos) e exposição do JWKS para o back-node.</li>
 *   <li>Refresh token opaco, rotativo, com detecção de reuso e janela de tolerância para
 *       refresh concorrente.</li>
 * </ul>
 */
package com.quickorder.restaurant.core.equipe;
