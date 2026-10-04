package uy.edu.um.entities;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Central observable state. Updates are protected with a ReentrantReadWriteLock.
 * Every update produces an immutable snapshot and sends it to the display queue.
 */
public class RestaurantMonitor {
    private final long startTime = System.currentTimeMillis();
    private final BlockingQueue<SimulationEvent> eventQueue;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<Integer, String> clients = new TreeMap<>();
    private final Map<Integer, String> waiters = new TreeMap<>();
    private final Map<Integer, String> cooks = new TreeMap<>();
    private final Map<Integer, String> cashiers = new TreeMap<>();
    private final Map<Integer, String> tables = new TreeMap<>();

    public RestaurantMonitor(BlockingQueue<SimulationEvent> eventQueue) {
        this.eventQueue = eventQueue;
    }

    public void initializeRestaurant(int tableCount, int waiterCount, int cookCount, int cashierCount) {
        lock.writeLock().lock();
        try {
            for (int i = 1; i <= tableCount; i++) tables.put(i, "LIBRE");
            for (int i = 1; i <= waiterCount; i++) waiters.put(i, "INICIALIZANDO");
            for (int i = 1; i <= cookCount; i++) cooks.put(i, "INICIALIZANDO");
            for (int i = 1; i <= cashierCount; i++) cashiers.put(i, "INICIALIZANDO");
            publishLocked("Restaurante inicializado");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void client(int id, String state) {
        update(clients, id, state, "Cliente " + id + ": " + state);
    }

    public void waiter(int id, String state) {
        update(waiters, id, state, "Mozo " + id + ": " + state);
    }

    public void cook(int id, String state) {
        update(cooks, id, state, "Cocinero " + id + ": " + state);
    }

    public void cashier(int id, String state) {
        update(cashiers, id, state, "Cajero " + id + ": " + state);
    }

    public void table(int id, String state) {
        update(tables, id, state, "Mesa " + id + ": " + state);
    }

    public void event(String description) {
        lock.readLock().lock();
        try {
            eventQueue.offer(new SimulationEvent(
                    System.currentTimeMillis() - startTime,
                    description,
                    snapshotLocked()));
        } finally {
            lock.readLock().unlock();
        }
    }

    private void update(Map<Integer, String> map, int id, String state, String description) {
        lock.writeLock().lock();
        try {
            map.put(id, state);
            publishLocked(description);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void publishLocked(String description) {
        eventQueue.offer(new SimulationEvent(
                System.currentTimeMillis() - startTime,
                description,
                snapshotLocked()));
    }

    private String snapshotLocked() {
        return "CLIENTES " + clients
                + System.lineSeparator() + "MOZOS " + waiters
                + System.lineSeparator() + "COCINEROS " + cooks
                + System.lineSeparator() + "CAJEROS " + cashiers
                + System.lineSeparator() + "MESAS " + tables;
    }
}
