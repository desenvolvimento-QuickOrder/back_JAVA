/**
 * Multi-tenancy por linha ({@code restaurante_id}) com Row Level Security no PostgreSQL.
 *
 * <ul>
 *   <li>{@code TenantContext}: guarda o tenant da requisição, extraído da claim
 *       {@code tenant_id} do JWT. O tenant nunca vem do path nem do corpo, exceto o slug do
 *       cardápio público.</li>
 *   <li>Aplicação do tenant no início de cada transação com
 *       {@code set_config('app.tenant_id', :id, true)} (equivalente a {@code SET LOCAL}).
 *       Exige {@code spring.jpa.open-in-view=false}.</li>
 *   <li>RLS é a garantia de isolamento; {@code @TenantId} do Hibernate é apenas apoio.</li>
 *   <li>A aplicação conecta com o papel restrito {@code quickorder_app} (sem
 *       {@code BYPASSRLS} e sem ser dono das tabelas); o Flyway roda com o dono.</li>
 * </ul>
 */
package com.quickorder.restaurant.core.shared.tenant;
