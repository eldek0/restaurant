package uy.edu.um.entities.actors;

import uy.edu.um.entities.SimulationEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** Single consumer responsible for console output and the text log. */
public class Display implements Runnable {
    private final BlockingQueue<SimulationEvent> events;
    private final Path logFile;
    private volatile boolean active = true;

    public Display(BlockingQueue<SimulationEvent> events, Path logFile) {
        this.events = events;
        this.logFile = logFile;
    }

    @Override
    public void run() {
        try (BufferedWriter writer = Files.newBufferedWriter(
                logFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            while (active || !events.isEmpty()) {
                SimulationEvent event = events.poll(300, TimeUnit.MILLISECONDS);
                if (event == null) continue;
                String text = format(event);
                System.out.print(text);
                writer.write(text);
                writer.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir el log " + logFile, e);
        }
    }

    private static String format(SimulationEvent event) {
        return String.format(
                "%n[%06d ms] %s%n%s%n",
                event.elapsedMillis(),
                event.description(),
                event.snapshot());
    }

    public void stopWhenDrained() {
        active = false;
    }
}
