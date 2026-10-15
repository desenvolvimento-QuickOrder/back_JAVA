/**
 * Módulo mesa: cadastro das mesas do restaurante e status derivado.
 *
 * <p>O status ({@code LIVRE} ou {@code OCUPADA}) é derivado da existência de comanda aberta
 * e não é alterado diretamente pelo cliente da API. Mudanças geram {@code mesa.status_alterado}.
 */
package com.quickorder.restaurant.core.mesa;
