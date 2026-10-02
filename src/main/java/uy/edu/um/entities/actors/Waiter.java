package uy.edu.um.entities.actors;

import uy.edu.um.Config;
import uy.edu.um.entities.CookingTask;
import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Order;
import uy.edu.um.entities.RestaurantMonitor;
import uy.edu.um.entities.Table;
import uy.edu.um.entities.WaiterTask;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/** Any waiter can take an order, serve a ready order or clean a table. */
public class Waiter implements Runnable {
    private final int id;
    private final DiningRoom diningRoom;
    private final BlockingQueue<WaiterTask> waiterTasks;
    private final BlockingQueue<CookingTask> kitchenQueue;
    private final ExecutorService orderCompletionPool;
    private final RestaurantMonitor monitor;
    private volatile boolean active = true;

    public Waiter(int id,
                  DiningRoom diningRoom,
                  BlockingQueue<WaiterTask> waiterTasks,
                  BlockingQueue<CookingTask> kitchenQueue,
                  ExecutorService orderCompletionPool,
                  RestaurantMonitor monitor) {
        this.id = id;
        this.diningRoom = diningRoom;
        this.waiterTasks = waiterTasks;
        this.kitchenQueue = kitchenQueue;
        this.orderCompletionPool = orderCompletionPool;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        state("esperando tareas");
        try {
            while (active || !waiterTasks.isEmpty()) {
                WaiterTask task = waiterTasks.poll(300, TimeUnit.MILLISECONDS);
                if (task == null) continue;

                switch (task.getType()) {
                    case TAKE_ORDER -> takeOrder(task.getTable());
                    case SERVE_ORDER -> serve(task.getOrder());
                    case CLEAN_TABLE -> clean(task.getTable());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            state("finalizado");
        }
    }

    private void takeOrder(Table table) throws InterruptedException {
        state("tomando pedido mesa " + table.getId());
        Thread.sleep(randomTime(Config.TZmin, Config.TZmax));

        if (!table.markOrderTaken()) {
            state("pedido mesa " + table.getId() + " cancelado por cierre");
            return;
        }

        List<Menu> menus = table.getChoices();
        Order order = new Order(table, menus);
        if (monitor != null) monitor.table(table.getId(), "PEDIDO EN COCINA");

        // Message passing: one task per dish, inserted together in FIFO order.
        for (Menu menu : menus) kitchenQueue.put(new CookingTask(order, menu));

        // CountDownLatch barrier: a separate task waits for all P dishes.
        orderCompletionPool.submit(() -> {
            try {
                order.awaitAllDishes();
                waiterTasks.put(WaiterTask.serve(order));
                if (monitor != null)
                    monitor.table(order.getTableId(), "PLATOS PRONTOS - esperando mozo");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        state("esperando tareas");
    }

    private void serve(Order order) throws InterruptedException {
        state("sirviendo mesa " + order.getTableId());
        Thread.sleep(randomTime(Config.TRmin, Config.TRmax));
        order.getTable().markFoodServed();
        if (monitor != null) monitor.table(order.getTableId(), "SERVIDA - clientes comiendo");
        state("esperando tareas");
    }

    private void clean(Table table) throws InterruptedException {
        state("limpiando mesa " + table.getId());
        Thread.sleep(randomTime(Config.TLmin, Config.TLmax));
        diningRoom.markClean(table);
        state("esperando tareas");
    }

    public void stop() {
        active = false;
    }

    private void state(String state) {
        if (monitor != null) monitor.waiter(id, state);
    }

    private static int randomTime(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
