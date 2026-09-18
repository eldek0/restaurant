package uy.edu.um.entities.actors;

import uy.edu.um.Config;
import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.Table;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/** One client follows the full restaurant lifecycle in its own thread. */
public class Client implements Runnable {
    private final int id;
    private final DiningRoom diningRoom;
    private final List<Menu> availableMenus;
    private final BlockingQueue<Payment> paymentQueue;

    public Client(int id, DiningRoom diningRoom, List<Menu> availableMenus,
                  BlockingQueue<Payment> paymentQueue) {
        if (availableMenus.isEmpty()) throw new IllegalArgumentException("menu list is empty");
        this.id = id;
        this.diningRoom = diningRoom;
        this.availableMenus = List.copyOf(availableMenus);
        this.paymentQueue = paymentQueue;
    }

    @Override
    public void run() {
        Table table = null;
        try {
            DiningRoom.Seat seat = diningRoom.enter();
            if (seat == null) return;
            table = seat.table();
            System.out.printf("Client %d seated at table %d%n", id, table.getId());

            Thread.sleep(randomTime(Config.TMmin, Config.TMmax));
            Menu choice = availableMenus.get(ThreadLocalRandom.current().nextInt(availableMenus.size()));
            table.selectMenu(seat.number(), choice);
            System.out.printf("Client %d chose %s%n", id, choice.getName());

            if (!table.getOrderTaken().get()) return;
            table.getFoodServed().get();
            Thread.sleep(randomTime(Config.TQmin, Config.TQmax));
            System.out.printf("Client %d finished eating%n", id);

            Payment payment = new Payment(id, table.getId(), choice);
            paymentQueue.put(payment);
            payment.getCompleted().get();
            System.out.printf("Client %d paid and left%n", id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (table != null) table.cancelIfNotOrdered();
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("client handoff failed", e);
        } finally {
            if (table != null) diningRoom.leave(table);
        }
    }

    private static int randomTime(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
