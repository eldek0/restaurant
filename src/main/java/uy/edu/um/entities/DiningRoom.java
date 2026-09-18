package uy.edu.um.entities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/** Assigns complete groups to clean tables and keeps excess clients outside. */
public class DiningRoom {
    public record Seat(Table table, int number) {}

    private static class WaitingClient {
        Seat seat;
    }

    private final Object lock = new Object();
    private final int peoplePerTable;
    private final ArrayDeque<Integer> freeTables = new ArrayDeque<>();
    private final ArrayDeque<WaitingClient> waiting = new ArrayDeque<>();
    private final Map<Integer, Table> occupied = new HashMap<>();
    private final BlockingQueue<Table> waiterRequests;
    private final BlockingQueue<Table> cleaningRequests;
    private boolean open = true;

    public DiningRoom(int tables, int peoplePerTable, BlockingQueue<Table> waiterRequests,
                      BlockingQueue<Table> cleaningRequests) {
        if (tables <= 0 || peoplePerTable <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.peoplePerTable = peoplePerTable;
        this.waiterRequests = waiterRequests;
        this.cleaningRequests = cleaningRequests;
        for (int i = 1; i <= tables; i++) freeTables.addLast(i);
    }

    /** Returns null if the restaurant closes before the client gets a seat. */
    public Seat enter() throws InterruptedException {
        synchronized (lock) {
            if (!open) return null;
            WaitingClient client = new WaitingClient();
            waiting.addLast(client);
            assignGroups();
            try {
                while (client.seat == null && open) lock.wait();
                return client.seat;
            } catch (InterruptedException e) {
                if (client.seat == null) waiting.remove(client);
                else {
                    client.seat.table().cancelIfNotOrdered();
                    leave(client.seat.table());
                }
                throw e;
            }
        }
    }

    private void assignGroups() {
        while (open && !freeTables.isEmpty() && waiting.size() >= peoplePerTable) {
            int id = freeTables.removeFirst();
            Table table = new Table(id, peoplePerTable, waiterRequests);
            occupied.put(id, table);
            for (int seat = 0; seat < peoplePerTable; seat++)
                waiting.removeFirst().seat = new Seat(table, seat);
            lock.notifyAll();
        }
    }

    /** Called by each client after payment or cancellation. */
    public void leave(Table table) {
        if (table.clientDeparted()) cleaningRequests.add(table);
    }

    /** Called by a waiter after lifting dishes and cleaning the empty table. */
    public void markClean(Table table) {
        synchronized (lock) {
            if (occupied.get(table.getId()) != table || !table.isReadyForCleaning())
                throw new IllegalArgumentException("table is not occupied by this group");
            occupied.remove(table.getId());
            freeTables.addLast(table.getId());
            assignGroups();
        }
    }

    /** Closes admissions and cancels groups that have not placed an order. */
    public void close() {
        List<Table> current;
        synchronized (lock) {
            open = false;
            waiting.clear();
            current = new ArrayList<>(occupied.values());
            lock.notifyAll();
        }
        current.forEach(Table::cancelIfNotOrdered);
    }
}
