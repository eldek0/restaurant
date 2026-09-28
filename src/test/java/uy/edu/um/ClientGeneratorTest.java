package uy.edu.um;

import uy.edu.um.entities.DiningRoom;
import uy.edu.um.entities.Menu;
import uy.edu.um.entities.Payment;
import uy.edu.um.entities.Table;
import uy.edu.um.entities.actors.ClientGenerator;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Run with javac/java; verifies generation and stopping admissions. */
public class ClientGeneratorTest {
    public static void main(String[] args) throws Exception {
        BlockingQueue<Table> waiterRequests = new LinkedBlockingQueue<>();
        BlockingQueue<Table> cleaningRequests = new LinkedBlockingQueue<>();
        BlockingQueue<Payment> payments = new LinkedBlockingQueue<>();
        DiningRoom room = new DiningRoom(1, 2, waiterRequests, cleaningRequests);
        ExecutorService clientPool = Executors.newFixedThreadPool(4);
        ClientGenerator generator = new ClientGenerator(
                room, List.of(new Menu("Test", 1, 1)), payments, clientPool, 20, 30);
        Thread generatorThread = new Thread(generator, "client-generator-test");

        generatorThread.start();
        Thread.sleep(180);
        generator.stop();
        generatorThread.join(1000);
        if (generatorThread.isAlive()) throw new AssertionError("generator did not stop");

        int generatedAtClose = generator.getGeneratedCount();
        if (generatedAtClose < 3) throw new AssertionError("too few clients generated");
        Thread.sleep(80);
        if (generator.getGeneratedCount() != generatedAtClose)
            throw new AssertionError("client generated after stop");

        room.close();
        clientPool.shutdownNow();
        if (!clientPool.awaitTermination(2, TimeUnit.SECONDS))
            throw new AssertionError("clients did not stop after closure");
        System.out.println("ClientGeneratorTest passed");
    }
}
