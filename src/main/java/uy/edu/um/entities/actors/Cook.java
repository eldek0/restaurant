package uy.edu.um.entities.actors;

import uy.edu.um.entities.CookingTask;
import uy.edu.um.entities.RestaurantMonitor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Consumer of dish-level cooking tasks. Since all tasks of an order are enqueued
 * together before the next order, free cooks always receive dishes in order-arrival order.
 */
public class Cook implements Runnable {
    private final int id;
    private final BlockingQueue<CookingTask> kitchenQueue;
    private final RestaurantMonitor monitor;
    private volatile boolean active = true;

    public Cook(int id, BlockingQueue<CookingTask> kitchenQueue, RestaurantMonitor monitor) {
        this.id = id;
        this.kitchenQueue = kitchenQueue;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        state("esperando pedidos");
        try {
            while (active || !kitchenQueue.isEmpty()) {
                CookingTask task = kitchenQueue.poll(300, TimeUnit.MILLISECONDS);
                if (task == null) continue;

                state("cocinando " + task.menu().getName()
                        + " para mesa " + task.order().getTableId());
                Thread.sleep(task.menu().cookingTime());
                task.order().dishFinished();
                state("plato " + task.menu().getName()
                        + " pronto para mesa " + task.order().getTableId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            state("finalizado");
        }
    }

    public void stop() {
        active = false;
    }

    private void state(String state) {
        if (monitor != null) monitor.cook(id, state);
    }
}
