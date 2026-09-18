package uy.edu.um;

import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.Table;
import uy.edu.um.entities.actors.Client;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Run with javac/java; exercises two successive groups on one table. */
public class ClientFlowTest {
    public static void main(String[] args) throws Exception {
        BlockingQueue<Table> requests = new LinkedBlockingQueue<>();
        BlockingQueue<Table> cleaning = new LinkedBlockingQueue<>();
        BlockingQueue<Payment> payments = new LinkedBlockingQueue<>();
        DiningRoom room = new DiningRoom(1, 2, requests, cleaning);
        ExecutorService clients = Executors.newFixedThreadPool(4);
        Menu menu = new Menu("Test", 1, 1);
        try {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++)
                futures.add(clients.submit(new Client(i, room, List.of(menu), payments)));
            for (int group = 0; group < 2; group++) {
                Table table = require(requests.poll(3, TimeUnit.SECONDS), "missing waiter request");
                check(table.getChoices().size() == 2, "incomplete group");
                check(table.markOrderTaken(), "order was cancelled");
                table.markFoodServed();
                for (int i = 0; i < 2; i++)
                    require(payments.poll(4, TimeUnit.SECONDS), "missing payment").complete();
                Table dirty = require(cleaning.poll(3, TimeUnit.SECONDS), "missing cleaning request");
                check(dirty == table, "wrong table cleaned");
                room.markClean(dirty);
            }
            for (Future<?> future : futures) future.get(3, TimeUnit.SECONDS);
            room.close();

            DiningRoom closing = new DiningRoom(1, 2, new LinkedBlockingQueue<>(), new LinkedBlockingQueue<>());
            ExecutorService entrants = Executors.newFixedThreadPool(2);
            try {
                Future<DiningRoom.Seat> first = entrants.submit(closing::enter);
                Future<DiningRoom.Seat> second = entrants.submit(closing::enter);
                Table table = require(first.get(2, TimeUnit.SECONDS).table(), "first seat missing");
                check(second.get(2, TimeUnit.SECONDS).table() == table, "group split across tables");
                closing.close();
                check(!table.getOrderTaken().get(2, TimeUnit.SECONDS), "unplaced order survived closure");
            } finally {
                entrants.shutdownNow();
            }
            System.out.println("ClientFlowTest passed");
        } finally {
            room.close();
            clients.shutdownNow();
        }
    }

    private static <T> T require(T value, String message) {
        if (value == null) throw new AssertionError(message);
        return value;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
