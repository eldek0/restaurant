package uy.edu.um.entities;

public class Dish {
    private final int tableId;
    private final Menu menu;
    private final long timestamp;

    public Dish(int tableId, Menu menu) {
        this.tableId = tableId;
        this.menu = menu;
        this.timestamp = System.currentTimeMillis();
    }

    public int getTableId() {
        return tableId;
    }

    public Menu getMenu() {
        return menu;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Dish{" +
                "tableId=" + tableId +
                ", menu=" + menu +
                ", timestamp=" + timestamp +
                '}';
    }
}
