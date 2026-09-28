package uy.edu.um.entities.actors;

import uy.edu.um.Config;
import uy.edu.um.entities.Payment;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Takes clients from the restaurant's single payment queue.
 * Call stop() only after no more clients can join the queue.
 */
public class Cashier implements Runnable {
    private final int id;
    private final BlockingQueue<Payment> paymentQueue;
    private volatile boolean active = true;

    public Cashier(int id, BlockingQueue<Payment> paymentQueue) {
        if (paymentQueue == null) throw new IllegalArgumentException("payment queue is required");
        this.id = id;
        this.paymentQueue = paymentQueue;
    }

    @Override
    public void run() {
        try {
            while (active || !paymentQueue.isEmpty()) {
                Payment payment = paymentQueue.poll(500, TimeUnit.MILLISECONDS);
                if (payment == null) continue;

                System.out.printf("Cashier %d charging client %d from table %d%n",
                        id, payment.getClientId(), payment.getTableId());
                Thread.sleep(randomTime(Config.TYmin, Config.TYmax));
                payment.complete();
                System.out.printf("Cashier %d charged client %d%n", id, payment.getClientId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        active = false;
    }

    private static int randomTime(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
