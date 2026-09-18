package uy.edu.um.entities;

import java.util.concurrent.CompletableFuture;

/** A request in the single cashier queue. The cashier calls complete() after charging. */
public class Payment {
    private final int clientId;
    private final int tableId;
    private final Menu menu;
    private final CompletableFuture<Void> completed = new CompletableFuture<>();

    public Payment(int clientId, int tableId, Menu menu) {
        this.clientId = clientId;
        this.tableId = tableId;
        this.menu = menu;
    }

    public int getClientId() { return clientId; }
    public int getTableId() { return tableId; }
    public Menu getMenu() { return menu; }
    public CompletableFuture<Void> getCompleted() { return completed; }
    public void complete() { completed.complete(null); }
}
