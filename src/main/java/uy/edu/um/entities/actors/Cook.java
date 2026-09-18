package uy.edu.um.entities.actors;

import uy.edu.um.entities.Dish;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Order;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Cook implements Runnable{
    private final int id;
    private final BlockingQueue<Order> orderQueue;
    private final BlockingQueue<Dish> dishesReadyQueue;
    private volatile boolean active = true;

    public Cook(int id, BlockingQueue<Order> orderQueue, BlockingQueue<Dish> dishesReadyQueue) {
        this.id = id;
        this.orderQueue = orderQueue;
        this.dishesReadyQueue = dishesReadyQueue;
    }

    @Override
    public void run() {
        try {
            while (this.active || !orderQueue.isEmpty()) {
                Order order = orderQueue.poll(500, TimeUnit.MILLISECONDS);
                if (order == null) continue;
                for (Menu m : order.getMenus()) {
                    Thread.sleep(m.cookingTime());
                    dishesReadyQueue.put(new Dish(order.getTableId(), m));
                    order.getDishesFinished().countDown();
                    System.out.printf("Cook %d: %s ready for table %d%n", id, m.getName(), order.getTableId());
                }
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void stop(){
        active = false;
    }
}
