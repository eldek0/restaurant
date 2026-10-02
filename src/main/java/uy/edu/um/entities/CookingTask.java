package uy.edu.um.entities;

/** One dish belonging to an order. Tasks are queued in order of order arrival. */
public record CookingTask(Order order, Menu menu) {
}
