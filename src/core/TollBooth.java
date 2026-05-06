package core;

import model.Vehicle;

import java.util.concurrent.Semaphore;

public class TollBooth {

    private final Semaphore semaphore;
    private final String name;

    public TollBooth(String name) {
        this.name = name;
        this.semaphore = new Semaphore(1); // only 1 vehicle at a time
    }

    // Vehicle enters toll
    public void enter(Vehicle vehicle) throws InterruptedException {
        semaphore.acquire(); // lock
    }

    // Vehicle exits toll
    public void exit(Vehicle vehicle) {
        semaphore.release(); // unlock
    }
}