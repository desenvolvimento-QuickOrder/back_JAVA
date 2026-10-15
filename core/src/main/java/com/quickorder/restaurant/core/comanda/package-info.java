/**
 * Módulo comanda: comandas, pedidos (rodadas), itens, máquina de estados e fila da copa.
 *
 * <ul>
 *   <li>Uma comanda aberta por mesa (índice único parcial).</li>
 *   <li>Status existe só no pedido: {@code ENVIADO_COPA}, {@code EM_PREPARO}, {@code PRONTO},
 *       {@code ENTREGUE}, {@code CANCELADO}. Cancelamento só em {@code ENVIADO_COPA}.</li>
 *   <li>Novos itens após o envio viram um novo pedido (rodada).</li>
 *   <li>Fechar exige todos os pedidos entregues ou cancelados.</li>
 *   <li>Criar pedido, fechar e transferir bloqueiam a comanda antes dos pedidos.</li>
 * </ul>
 */
package com.quickorder.restaurant.core.comanda;
