/**
 * Testes do core.
 *
 * <ul>
 *   <li>Unitários ({@code *Test}, Surefire): domínio e casos de uso, sem Spring.
 *   <li>Integração ({@code *IT}, Failsafe): Testcontainers (PostgreSQL e Redis), migration {@code
 *       V1} com RLS e teste de isolamento entre tenants rodando com o papel restrito {@code
 *       quickorder_app}.
 *   <li>Fronteiras dos módulos com Spring Modulith ({@code ApplicationModules.verify()}).
 * </ul>
 */
package com.quickorder.restaurant.core;
