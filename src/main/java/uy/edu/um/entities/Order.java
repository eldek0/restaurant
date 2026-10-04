package uy.edu.um.entities;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/** A complete table order. CountDownLatch acts as the barrier for all P dishes. */
public class Order {
    private final Table table;
    private final int tableId;
    private final List<Menu> menus;
    private final CountDownLatch dishesFinished;

    public Order(Table table, List<Menu> menus) {
        if (table == null || menus == null || menus.isEmpty())
            throw new IllegalArgumentException("table and menus are required");
        this.table = table;
        this.tableId = table.getId();
        this.menus = List.copyOf(menus);
        this.dishesFinished = new CountDownLatch(menus.size());
    }

    /** Convenience constructor retained for small isolated tests. */
    public Order(int tableId, List<Menu> menus) {
        if (menus == null || menus.isEmpty())
            throw new IllegalArgumentException("menus are required");
        this.table = null;
        this.tableId = tableId;
        this.menus = List.copyOf(menus);
        this.dishesFinished = new CountDownLatch(menus.size());
    }

    public Table getTable() { return table; }
    public int getTableId() { return tableId; }
    public List<Menu> getMenus() { return menus; }
    public CountDownLatch getDishesFinished() { return dishesFinished; }

    public void dishFinished() {
        dishesFinished.countDown();
    }

    public void awaitAllDishes() throws InterruptedException {
        dishesFinished.await();
    }
}
