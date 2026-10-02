package uy.edu.um;

import uy.edu.um.entities.CookingTask;
import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.RestaurantMonitor;
import uy.edu.um.entities.SimulationEvent;
import uy.edu.um.entities.WaiterTask;
import uy.edu.um.entities.actors.Cashier;
import uy.edu.um.entities.actors.ClientGenerator;
import uy.edu.um.entities.actors.Cook;
import uy.edu.um.entities.actors.Display;
import uy.edu.um.entities.actors.Waiter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        BlockingQueue<SimulationEvent> displayQueue = new LinkedBlockingQueue<>();
        BlockingQueue<WaiterTask> waiterTasks = new LinkedBlockingQueue<>();
        BlockingQueue<CookingTask> kitchenQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Payment> paymentQueue = new LinkedBlockingQueue<>();

        RestaurantMonitor monitor = new RestaurantMonitor(displayQueue);
        monitor.initializeRestaurant(Config.M, Config.Z, Config.C, Config.Y);

        Display display = new Display(displayQueue, Path.of("simulation.log"));
        ExecutorService displayPool = Executors.newSingleThreadExecutor();
        displayPool.submit(display);

        DiningRoom diningRoom = new DiningRoom(Config.M, Config.P, waiterTasks, monitor);

        // Each menu has its own cooking-time range, as required by the statement.
        List<Menu> menus = List.of(
                new Menu("Milanesa", 700, 1200),
                new Menu("Pasta", 500, 900),
                new Menu("Ensalada", 300, 600)
        );

        ExecutorService waiterPool = Executors.newFixedThreadPool(Config.Z);
        ExecutorService cookPool = Executors.newFixedThreadPool(Config.C);
        ExecutorService cashierPool = Executors.newFixedThreadPool(Config.Y);
        ExecutorService clientPool = Executors.newCachedThreadPool();
        ExecutorService orderCompletionPool = Executors.newCachedThreadPool();
        ExecutorService generatorPool = Executors.newSingleThreadExecutor();

        List<Waiter> waiters = new ArrayList<>();
        for (int i = 1; i <= Config.Z; i++) {
            Waiter waiter = new Waiter(i, diningRoom, waiterTasks, kitchenQueue,
                    orderCompletionPool, monitor);
            waiters.add(waiter);
            waiterPool.submit(waiter);
        }

        List<Cook> cooks = new ArrayList<>();
        for (int i = 1; i <= Config.C; i++) {
            Cook cook = new Cook(i, kitchenQueue, monitor);
            cooks.add(cook);
            cookPool.submit(cook);
        }

        List<Cashier> cashiers = new ArrayList<>();
        for (int i = 1; i <= Config.Y; i++) {
            Cashier cashier = new Cashier(i, paymentQueue, monitor);
            cashiers.add(cashier);
            cashierPool.submit(cashier);
        }

        ClientGenerator generator = new ClientGenerator(
                diningRoom, menus, paymentQueue, clientPool, monitor);
        Future<?> generatorFuture = generatorPool.submit(generator);

        monitor.event("RESTAURANTE ABIERTO");
        Thread.sleep(Config.T);

        // T: stop arrivals first, then close admissions and cancel not-yet-placed orders.
        generator.stop();
        generatorFuture.get(2, TimeUnit.SECONDS);
        generatorPool.shutdown();
        diningRoom.close();

        // No new clients can be created now; clients with accepted orders finish normally.
        clientPool.shutdown();
        awaitOrFail(clientPool, 120, "clientes");

        // The last departing group may still be waiting for a waiter to clean its table.
        diningRoom.awaitEmpty();

        // All clients have finished; therefore no future payments/orders can be produced.
        orderCompletionPool.shutdown();
        awaitOrFail(orderCompletionPool, 30, "barreras de pedidos");

        cashiers.forEach(Cashier::stop);
        cooks.forEach(Cook::stop);
        waiters.forEach(Waiter::stop);

        cashierPool.shutdown();
        cookPool.shutdown();
        waiterPool.shutdown();

        awaitOrFail(cashierPool, 10, "cajeros");
        awaitOrFail(cookPool, 10, "cocineros");
        awaitOrFail(waiterPool, 10, "mozos");

        monitor.event("SIMULACION FINALIZADA");
        display.stopWhenDrained();
        displayPool.shutdown();
        awaitOrFail(displayPool, 10, "display");

        System.out.println("\nLog guardado en simulation.log");
    }

    private static void awaitOrFail(ExecutorService pool, long seconds, String name)
            throws InterruptedException {
        if (!pool.awaitTermination(seconds, TimeUnit.SECONDS)) {
            pool.shutdownNow();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                throw new IllegalStateException("No finalizaron los hilos de " + name);
        }
    }
}
