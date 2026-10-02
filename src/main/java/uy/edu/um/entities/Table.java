package uy.edu.um.entities;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

/** One group of P clients occupies a table until payment and cleaning finish. */
public class Table {
    private final int id;
    private final Menu[] choices;
    private final BlockingQueue<WaiterTask> waiterTasks;
    private final CompletableFuture<Boolean> orderTaken = new CompletableFuture<>();
    private final CompletableFuture<Void> foodServed = new CompletableFuture<>();
    private int chosen;
    private int departed;
    private boolean cleaningRequested;

    public Table(int id, int seats, BlockingQueue<WaiterTask> waiterTasks) {
        if (seats <= 0) throw new IllegalArgumentException("seats must be positive");
        this.id = id;
        this.choices = new Menu[seats];
        this.waiterTasks = waiterTasks;
    }

    public int getId() { return id; }
    public int getSeats() { return choices.length; }
    public CompletableFuture<Boolean> getOrderTaken() { return orderTaken; }
    public CompletableFuture<Void> getFoodServed() { return foodServed; }

    /** Seat numbers are assigned by DiningRoom and are unique within this group. */
    public synchronized void selectMenu(int seat, Menu menu) {
        if (seat < 0 || seat >= choices.length || choices[seat] != null || menu == null)
            throw new IllegalArgumentException("invalid seat or choice");
        if (orderTaken.isDone()) return; // Restaurant closed before the waiter took the order.
        choices[seat] = menu;
        if (++chosen == choices.length) waiterTasks.offer(WaiterTask.takeOrder(this));
    }

    public synchronized List<Menu> getChoices() {
        if (chosen != choices.length) throw new IllegalStateException("group has not chosen yet");
        return List.copyOf(Arrays.asList(choices));
    }

    /** Returns true only if the waiter wins the race against restaurant closure. */
    public synchronized boolean markOrderTaken() {
        return orderTaken.complete(true);
    }

    /** Closure releases groups whose order was not yet sent to the kitchen. */
    public synchronized boolean cancelIfNotOrdered() {
        return orderTaken.complete(false);
    }

    /** Call only after all P dishes have been served to this table. */
    public void markFoodServed() {
        foodServed.complete(null);
    }

    /** True exactly once, when the last client has left. */
    public synchronized boolean clientDeparted() {
        if (departed >= choices.length) throw new IllegalStateException("too many departures");
        departed++;
        if (departed == choices.length && !cleaningRequested) {
            cleaningRequested = true;
            return true;
        }
        return false;
    }

    public synchronized boolean isReadyForCleaning() {
        return cleaningRequested;
    }
}
