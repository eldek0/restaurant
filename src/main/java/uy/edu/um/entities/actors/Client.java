package uy.edu.um.entities.actors;

import uy.edu.um.Config;
import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.RestaurantMonitor;
import uy.edu.um.entities.Table;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

/** One client follows the full restaurant lifecycle in its own task. */
public class Client implements Runnable {
    private final int id;
    private final DiningRoom diningRoom;
    private final List<Menu> availableMenus;
    private final BlockingQueue<Payment> paymentQueue;
    private final RestaurantMonitor monitor;

    public Client(int id, DiningRoom diningRoom, List<Menu> availableMenus,
                  BlockingQueue<Payment> paymentQueue, RestaurantMonitor monitor) {
        if (availableMenus == null || availableMenus.isEmpty())
            throw new IllegalArgumentException("menu list is empty");
        this.id = id;
        this.diningRoom = diningRoom;
        this.availableMenus = List.copyOf(availableMenus);
        this.paymentQueue = paymentQueue;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        Table table = null;
        try {
            state("esperando afuera");
            DiningRoom.Seat seat = diningRoom.enter();
            if (seat == null) {
                state("se retira por cierre antes de ingresar");
                return;
            }

            table = seat.table();
            state("sentado en mesa " + table.getId() + " - eligiendo menú");

            Thread.sleep(randomTime(Config.TMmin, Config.TMmax));
            Menu choice = availableMenus.get(ThreadLocalRandom.current().nextInt(availableMenus.size()));
            table.selectMenu(seat.number(), choice);
            state("eligió " + choice.getName() + " - esperando mozo");

            if (!table.getOrderTaken().get()) {
                state("se retira: el restaurante cerró antes del pedido");
                return;
            }

            state("pedido realizado - esperando comida");
            table.getFoodServed().get();

            state("comiendo en mesa " + table.getId());
            Thread.sleep(randomTime(Config.TQmin, Config.TQmax));

            state("en cola de caja");
            Payment payment = new Payment(id, table.getId(), choice);
            paymentQueue.put(payment);
            payment.getCompleted().get();

            state("pagó y se retiró");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (table != null) table.cancelIfNotOrdered();
            state("interrumpido");
        } catch (ExecutionException e) {
            throw new IllegalStateException("client handoff failed", e);
        } finally {
            if (table != null) diningRoom.leave(table);
        }
    }

    private void state(String state) {
        if (monitor != null) monitor.client(id, state);
    }

    private static int randomTime(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
