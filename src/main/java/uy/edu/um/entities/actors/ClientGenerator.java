package uy.edu.um.entities.actors;

import uy.edu.um.Config;
import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.RestaurantMonitor;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates clients at random intervals while admissions are open. */
public class ClientGenerator implements Runnable {
    private final DiningRoom diningRoom;
    private final List<Menu> availableMenus;
    private final BlockingQueue<Payment> paymentQueue;
    private final ExecutorService clientPool;
    private final RestaurantMonitor monitor;
    private final int arrivalMin;
    private final int arrivalMax;
    private final AtomicInteger nextClientId = new AtomicInteger(1);
    private final Object lifecycleLock = new Object();
    private volatile boolean active = true;
    private volatile Thread runner;

    public ClientGenerator(DiningRoom diningRoom, List<Menu> availableMenus,
                           BlockingQueue<Payment> paymentQueue, ExecutorService clientPool,
                           RestaurantMonitor monitor) {
        this(diningRoom, availableMenus, paymentQueue, clientPool, monitor,
                Config.TPmin, Config.TPmax);
    }

    public ClientGenerator(DiningRoom diningRoom, List<Menu> availableMenus,
                           BlockingQueue<Payment> paymentQueue, ExecutorService clientPool,
                           RestaurantMonitor monitor, int arrivalMin, int arrivalMax) {
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
        this.monitor = monitor;
        this.arrivalMin = arrivalMin;
        this.arrivalMax = arrivalMax;
    }

    @Override
    public void run() {
        runner = Thread.currentThread();
        try {
            while (active) {
                Thread.sleep(randomTime(arrivalMin, arrivalMax));
                synchronized (lifecycleLock) {
                    if (!active) break;
                    int id = nextClientId.getAndIncrement();
                    if (monitor != null) monitor.client(id, "llegó al restaurante");
                    clientPool.submit(new Client(id, diningRoom, availableMenus, paymentQueue, monitor));
                }
            }
        } catch (InterruptedException e) {
            if (active) Thread.currentThread().interrupt();
        } finally {
            runner = null;
        }
    }

    public void stop() {
        synchronized (lifecycleLock) {
            active = false;
            Thread r = runner;
            if (r != null) r.interrupt();
        }
    }

    public int getGeneratedCount() {
        return nextClientId.get() - 1;
    }

    private static int randomTime(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
