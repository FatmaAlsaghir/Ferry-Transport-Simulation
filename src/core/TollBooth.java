package core;

import model.Vehicle;

import java.util.concurrent.Semaphore;

public class TollBooth {

    private final Semaphore semaphore;
    private final String name; // for logging later

    public TollBooth(String name) {
        this.name = name;
        this.semaphore = new Semaphore(1); // only 1 vehicle at a time
    }

    // Vehicle enters toll
    public void enter(Vehicle vehicle) throws InterruptedException {
        semaphore.acquire(); // lock
        System.out.println(vehicle + " entered toll " + name);
    }

    // Vehicle exits toll
    public void exit(Vehicle vehicle) {
        System.out.println(vehicle + " exited toll " + name);
        semaphore.release(); // unlock
    }
}
