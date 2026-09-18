package uy.edu.um.entities;

import java.util.concurrent.ThreadLocalRandom;

public class Menu {
    private final String name;
    private final int tCmin;
    private final int tCmax;
    public Menu(String name, int tCmin, int tCmax){
        this.name = name;
        this.tCmin = tCmin;
        this.tCmax = tCmax;
    }

    public int cookingTime() {
        return ThreadLocalRandom.current().nextInt(this.tCmin, this.tCmax + 1);
    }

    public String getName() {
        return name;
    }

    public int getTCmin() {
        return tCmin;
    }

    public int getTCmax() {
        return tCmax;
    }
}
