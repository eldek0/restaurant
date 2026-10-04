package uy.edu.um.entities;

/** Immutable snapshot event consumed by the display/log thread. */
public record SimulationEvent(long elapsedMillis, String description, String snapshot) {
}
