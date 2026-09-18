package uy.edu.um.entities;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Order {
    private final int tableId;
    private final List<Menu> menus;
    private final CountDownLatch dishesFinished;

    public Order(int tableId, List<Menu> menus) {
        this.tableId = tableId;
        this.menus = menus;
        this.dishesFinished = new CountDownLatch(menus.size());
    }

    public int getTableId() {
        return tableId;
    }

    public List<Menu> getMenus() {
        return menus;
    }

    public CountDownLatch getDishesFinished() {
        return dishesFinished;
    }
}
