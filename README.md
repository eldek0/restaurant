# restaurant - Entregable 2

Simulación concurrente de un restaurante para Programación Avanzada.

## Flujo principal

1. `ClientGenerator` genera clientes a intervalos aleatorios y los ejecuta en un `ExecutorService`.
2. `DiningRoom` agrupa exactamente `Config.P` clientes y asigna únicamente mesas libres.
3. Cada `Client` elige un menú. Cuando todos los integrantes eligieron, `Table` publica una tarea `TAKE_ORDER`.
4. Cualquier `Waiter` puede tomar la tarea, registrar el pedido y publicar un `CookingTask` por plato.
5. Los `Cook` consumen los platos desde una `BlockingQueue`. El `Order` usa un `CountDownLatch` para representar la barrera de P platos.
6. Cuando la barrera llega a cero se publica una tarea `SERVE_ORDER`. Un mozo sirve la mesa y libera a todos los clientes para comer.
7. Cada cliente termina de comer de forma independiente, entra en la única `BlockingQueue<Payment>` y cualquier `Cashier` libre lo cobra.
8. Cuando sale el último cliente de una mesa, se publica una tarea `CLEAN_TABLE`. Una vez limpia, `DiningRoom` vuelve a ofrecerla.
9. Al cumplirse `Config.T` se detiene la generación y se cierran las puertas. Los grupos sin pedido se cancelan; los que ya pidieron terminan normalmente.
10. `Display` consume eventos con snapshots inmutables, imprime el estado completo y escribe `simulation.log`.

## Herramientas de concurrencia

- `synchronized` + `wait/notifyAll`: asignación de grupos/mesas y coordinación del comedor.
- `ReentrantReadWriteLock`: protección del estado observable de `RestaurantMonitor`.
- `AtomicInteger`: IDs de clientes generados concurrentemente.
- `CountDownLatch`: barrera de platos de cada pedido.
- `BlockingQueue`: pasaje de mensajes entre clientes, mozos, cocina, caja y display.
- `Executors`: pools de mozos, cocineros, cajeros, clientes, display y coordinación de pedidos.
- `CompletableFuture`: handoff puntual pedido tomado / comida servida / pago finalizado.
- `volatile`: señal de cierre de actores.
- Patrón Producer-Consumer: colas de tareas de mozos, cocina, caja y display.

## Ejecución

Con JDK 25 y Maven:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass=uy.edu.um.Main
```

Si no está configurado `exec-maven-plugin`, también puede ejecutarse `Main` desde el IDE.

El log queda en `simulation.log` en el directorio desde el que se inicia el programa.

## Configuración

Los parámetros de la letra se encuentran en `src/main/java/uy/edu/um/Config.java`.
Los menús y sus rangos de cocción se crean en `Main.java`.

## Pruebas incluidas

Las clases de `src/test/java` son pruebas ejecutables con método `main` para:
- cola única compartida por varios cajeros;
- flujo de dos grupos sucesivos usando la misma mesa;
- generación y detención de clientes.
