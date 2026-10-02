package uy.edu.um;

import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.Table;
import uy.edu.um.entities.WaiterTask;
import uy.edu.um.entities.actors.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientFlowTest {
    public static void main(String[] args) throws Exception {
        BlockingQueue<WaiterTask> waiterTasks = new LinkedBlockingQueue<>();
        BlockingQueue<Payment> payments = new LinkedBlockingQueue<>();
        DiningRoom room = new DiningRoom(1, 2, waiterTasks, null);
        ExecutorService clients = Executors.newFixedThreadPool(4);
        Menu menu = new Menu("Test", 1, 1);

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++)
                futures.add(clients.submit(new Client(i, room, List.of(menu), payments, null)));

            for (int group = 0; group < 2; group++) {
                WaiterTask take = require(waiterTasks.poll(3, TimeUnit.SECONDS), "missing order request");
                check(take.getType() == WaiterTask.Type.TAKE_ORDER, "wrong task");
                Table table = take.getTable();
                check(table.getChoices().size() == 2, "incomplete group");
                check(table.markOrderTaken(), "order was cancelled");
                table.markFoodServed();

                for (int i = 0; i < 2; i++)
                    require(payments.poll(4, TimeUnit.SECONDS), "missing payment").complete();

                WaiterTask clean = require(waiterTasks.poll(3, TimeUnit.SECONDS), "missing cleaning request");
                check(clean.getType() == WaiterTask.Type.CLEAN_TABLE, "wrong cleaning task");
                check(clean.getTable() == table, "wrong table cleaned");
                room.markClean(table);
            }

            for (Future<?> future : futures) future.get(3, TimeUnit.SECONDS);
            room.close();
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
