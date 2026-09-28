package uy.edu.um.entities.actors;

import uy.edu.um.Config;
import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates clients at random intervals while the restaurant is open. */
public class ClientGenerator implements Runnable {
    private final DiningRoom diningRoom;
    private final List<Menu> availableMenus;
    private final BlockingQueue<Payment> paymentQueue;
    private final ExecutorService clientPool;
    private final int arrivalMin;
    private final int arrivalMax;
    private final AtomicInteger nextClientId = new AtomicInteger(1);
    private final Object lifecycleLock = new Object();
    private volatile boolean active = true;

    public ClientGenerator(DiningRoom diningRoom, List<Menu> availableMenus,
                           BlockingQueue<Payment> paymentQueue, ExecutorService clientPool) {
        this(diningRoom, availableMenus, paymentQueue, clientPool, Config.TPmin, Config.TPmax);
    }

    public ClientGenerator(DiningRoom diningRoom, List<Menu> availableMenus,
                           BlockingQueue<Payment> paymentQueue, ExecutorService clientPool,
                           int arrivalMin, int arrivalMax) {
        if (diningRoom == null || paymentQueue == null || clientPool == null)
            throw new IllegalArgumentException("generator dependencies are required");
        if (availableMenus == null || availableMenus.isEmpty())
            throw new IllegalArgumentException("menu list is empty");
        if (arrivalMin < 0 || arrivalMax < arrivalMin)
            throw new IllegalArgumentException("invalid arrival interval");
        this.diningRoom = diningRoom;
        this.availableMenus = List.copyOf(availableMenus);
        this.paymentQueue = paymentQueue;
        this.clientPool = clientPool;
        this.arrivalMin = arrivalMin;
        this.arrivalMax = arrivalMax;
    }

    @Override
    public void run() {
        try {
            while (active) {
                Thread.sleep(randomTime(arrivalMin, arrivalMax));
                synchronized (lifecycleLock) {
                    if (!active) break;
                    int id = nextClientId.getAndIncrement();
                    clientPool.submit(new Client(id, diningRoom, availableMenus, paymentQueue));
                    System.out.printf("Client %d arrived at the restaurant%n", id);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        synchronized (lifecycleLock) {
            active = false;
        }
    }

    public int getGeneratedCount() {
        return nextClientId.get() - 1;
    }

    private static int randomTime(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
