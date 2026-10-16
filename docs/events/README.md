# Eventos do core

Contrato entre o core (publica) e o back-node (consome). Detalhes e decisões em
[`docs/arquitetura.md`](../arquitetura.md), seção 7.

- Broker: Redis Streams, um stream por agregado (`events:pedido`, `events:mesa`), consumer group `realtime`.
- Envelope: `eventId`, `tipo`, `schemaVersion`, `tenantId`, `payload`. Mudanças só aditivas.
- Pedido: `pedido.enviado`, `pedido.em_preparo`, `pedido.pronto`, `pedido.entregue`, `pedido.cancelado`; payload com `pedidoVersion`.
- Mesa: `mesa.status_alterado`.
- Rooms Socket.IO: `tenant:{id}:copa` e `tenant:{id}:salao`.
- Eventos são dicas: o front reconcilia pelo REST ao conectar e reconectar.
