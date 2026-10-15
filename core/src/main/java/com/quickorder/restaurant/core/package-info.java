/**
 * Core do QuickOrder: gestão de mesas, comandas e pedidos em tempo real para restaurantes.
 *
 * <p>Monolito modular com DDD tático e arquitetura hexagonal (portas e adaptadores). Cada
 * módulo de negócio é um subpacote direto deste e se divide em {@code domain},
 * {@code application}, {@code infrastructure} e {@code web}.
 *
 * <ul>
 *   <li>{@code shared}: kernel compartilhado (tenant, erros, eventos, web, segurança).</li>
 *   <li>{@code equipe}: login, JWT, usuários e papéis.</li>
 *   <li>{@code mesa}: cadastro e status das mesas.</li>
 *   <li>{@code cardapio}: categorias, produtos e disponibilidade.</li>
 *   <li>{@code comanda}: comandas, pedidos, itens, máquina de estados e fila da copa.</li>
 * </ul>
 *
 * <p>Regra central: o core é o único dono do estado do pedido. O back-node apenas consome
 * eventos e transmite notificações.
 *
 * <p>Contrato HTTP: {@code api/openapi.yaml} (contract-first, prefixo {@code /api/v1}).
 */
package com.quickorder.restaurant.core;
