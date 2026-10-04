package uy.edu.um.entities.actors;

import uy.edu.um.Config;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.RestaurantMonitor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/** Cashiers share one FIFO payment queue. */
public class Cashier implements Runnable {
    private final int id;
    private final BlockingQueue<Payment> paymentQueue;
    private final RestaurantMonitor monitor;
    private volatile boolean active = true;

    public Cashier(int id, BlockingQueue<Payment> paymentQueue, RestaurantMonitor monitor) {
        if (paymentQueue == null) throw new IllegalArgumentException("payment queue is required");
        this.id = id;
        this.paymentQueue = paymentQueue;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        state("esperando clientes");
        try {
            while (active || !paymentQueue.isEmpty()) {
                Payment payment = paymentQueue.poll(300, TimeUnit.MILLISECONDS);
                if (payment == null) continue;

                state("cobrando cliente " + payment.getClientId()
                        + " de mesa " + payment.getTableId());
                Thread.sleep(randomTime(Config.TYmin, Config.TYmax));
                payment.complete();
                state("esperando clientes");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            state("finalizado");
        }
    }

    public void stop() {
        active = false;
    }

    private void state(String state) {
        if (monitor != null) monitor.cashier(id, state);
    }

    private static int randomTime(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
