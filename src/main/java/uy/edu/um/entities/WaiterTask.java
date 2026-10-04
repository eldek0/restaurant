package uy.edu.um.entities;

/** Work items consumed by any available waiter. */
public final class WaiterTask {
    public enum Type { TAKE_ORDER, SERVE_ORDER, CLEAN_TABLE }

    private final Type type;
    private final Table table;
    private final Order order;

    private WaiterTask(Type type, Table table, Order order) {
        this.type = type;
        this.table = table;
        this.order = order;
    }

    public static WaiterTask takeOrder(Table table) {
        return new WaiterTask(Type.TAKE_ORDER, table, null);
    }

    public static WaiterTask serve(Order order) {
        return new WaiterTask(Type.SERVE_ORDER, order.getTable(), order);
    }

    public static WaiterTask clean(Table table) {
        return new WaiterTask(Type.CLEAN_TABLE, table, null);
    }

    public Type getType() { return type; }
    public Table getTable() { return table; }
    public Order getOrder() { return order; }
}
