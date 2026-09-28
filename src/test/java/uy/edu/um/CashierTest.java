package uy.edu.um;

import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.actors.Cashier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Run with javac/java; verifies that several cashiers drain one shared queue. */
public class CashierTest {
    public static void main(String[] args) throws Exception {
        BlockingQueue<Payment> queue = new LinkedBlockingQueue<>();
        List<Cashier> cashiers = List.of(new Cashier(0, queue), new Cashier(1, queue));
        ExecutorService pool = Executors.newFixedThreadPool(cashiers.size());
        cashiers.forEach(pool::submit);

        List<Payment> payments = new ArrayList<>();
        Menu menu = new Menu("Test", 1, 1);
        for (int i = 0; i < 6; i++) {
            Payment payment = new Payment(i, i / 2 + 1, menu);
            payments.add(payment);
            queue.put(payment);
        }

        for (Payment payment : payments)
            payment.getCompleted().get(4, TimeUnit.SECONDS);

        cashiers.forEach(Cashier::stop);
        pool.shutdown();
        if (!pool.awaitTermination(2, TimeUnit.SECONDS))
            throw new AssertionError("cashiers did not stop");
        if (!queue.isEmpty()) throw new AssertionError("payment queue was not drained");
        System.out.println("CashierTest passed");
    }
}
