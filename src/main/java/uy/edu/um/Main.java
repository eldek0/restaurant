package uy.edu.um;

import uy.edu.um.entities.Dish;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Order;
import uy.edu.um.entities.actors.Cook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Dish> dishQueue = new LinkedBlockingQueue<>();
        ExecutorService pool = Executors.newFixedThreadPool(Config.C);

        List<Cook> cooks = new ArrayList<>();
        for (int i = 0; i < Config.C; i++) {
            Cook c = new Cook(i, orderQueue, dishQueue);
            cooks.add(c);
            pool.submit(c);
        }

        // Pedidos para probar
        orderQueue.put(new Order(1, List.of(new Menu("Milanesa", 1000, 2000))));
        orderQueue.put(new Order(2, List.of(new Menu("Pasta", 800, 1500))));

        Thread.sleep(5000);
        cooks.forEach(Cook::stop);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
}
