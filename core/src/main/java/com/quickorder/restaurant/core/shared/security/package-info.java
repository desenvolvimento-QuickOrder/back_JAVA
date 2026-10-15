/**
 * Configuração de segurança HTTP.
 *
 * <p>Nesta fase da base, apenas os endpoints de health e info do Actuator são públicos e
 * todo o resto é negado. Quando o módulo {@code equipe} existir, a cadeia passa a validar o
 * JWT RS256 (resource server) e a impor os papéis declarados em {@code x-roles} no contrato.
 */
package com.quickorder.restaurant.core.shared.security;
