# restaurant

## Integración de clientes

`DiningRoom` agrupa a los clientes de a `Config.P`, asigna una mesa libre y deja a los demás esperando afuera. Cada `Client` corre en su propio hilo: elige un `Menu`, espera al mozo y a que la mesa esté servida, come, pasa por la cola única de pagos y se retira. Una mesa recién vuelve a estar disponible después de que todos salen y un mozo la limpia.

Para conectar los otros roles, compartir estas colas entre los actores:

- `BlockingQueue<Table> waiterRequests`: cuando todos los clientes eligieron, aparece la mesa. El mozo obtiene `table.getChoices()` y llama `table.markOrderTaken()` antes de enviar el pedido a cocina; si devuelve `false`, el pedido quedó cancelado por el cierre. Cuando estén servidos todos los platos, llama `table.markFoodServed()`.
- `BlockingQueue<Table> cleaningRequests`: aparece una mesa cuando sale el último cliente. El mozo la limpia y llama `diningRoom.markClean(table)`.
- `BlockingQueue<Payment> paymentQueue`: el cajero cobra al cliente indicado por `payment.getClientId()` y confirma con `payment.complete()`.

Al llegar a `Config.T`, detener la generación de clientes y llamar `diningRoom.close()`. Esto despierta a quienes esperan afuera y cancela a los grupos que aún no realizaron el pedido. Los pedidos ya enviados a cocina continúan normalmente. Mantener activos mozos y cajeros hasta que terminen esos clientes y se limpien las mesas.
