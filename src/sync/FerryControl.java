package sync;

import core.WaitingQueue;
import model.Vehicle;
import utils.Logger;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FerryControl {

    private final Lock lock = new ReentrantLock(true); // fair lock

    private final Condition canBoard = lock.newCondition();
    private final Condition ferryReady = lock.newCondition();
    private final Condition arrivalCondition = lock.newCondition();

    private int currentLoad = 0;
    private final int MAX_CAPACITY = 20;

    private boolean loading = true;
    private boolean unloading = false;
    private boolean arrived = false;

    // Vehicle requests boarding
    public void requestBoarding(Vehicle vehicle, WaitingQueue queue) throws InterruptedException {
        lock.lock();
        try {
            // Wait until boarding is allowed and vehicle fits
            while (!loading || unloading || !canFit(vehicle, queue)) {
                canBoard.await();
            }

            queue.dequeue(); // FIFO queue
            currentLoad += vehicle.getSize();

            // Boarding log
            Logger.vehicleBoarded(vehicle.toString(), currentLoad);

            ferryReady.signal(); // notify ferry thread

        } finally {
            lock.unlock();
        }
    }

    // Check capacity constraint
    private boolean canFit(Vehicle vehicle, WaitingQueue queue) {
        if (vehicle == null) return false;
        return currentLoad + vehicle.getSize() <= MAX_CAPACITY;
    }

    // Ferry waits until departure conditions are met
    public void waitForDeparture(WaitingQueue queue) throws InterruptedException {
        lock.lock();
        try {
            while (!shouldDepart(queue)) {
                ferryReady.awaitNanos(1_000_000_000); // timeout avoids starvation
            }
            loading = false; // stop boarding
        } finally {
            lock.unlock();
        }
    }

    // Departure conditions
    private boolean shouldDepart(WaitingQueue queue) {
        if (currentLoad == MAX_CAPACITY) return true;

        if (queue.isEmpty()) return currentLoad > 0;

        Vehicle next = queue.peek();
        return next != null && !canFit(next, queue);
    }

    // Vehicles wait until ferry arrives
    public void waitForArrival() throws InterruptedException {
        lock.lock();
        try {
            while (!arrived) {
                arrivalCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    // Ferry signals arrival
    public void signalArrival() {
        lock.lock();
        try {
            arrived = true;
            arrivalCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // Reset ferry for next trip
    public void resetAfterTrip() {
        lock.lock();
        try {
            currentLoad = 0;
            loading = true;
            unloading = false;
            arrived = false;

            canBoard.signalAll(); // allow boarding again

        } finally {
            lock.unlock();
        }
    }

    // Start unloading phase
    public void startUnloading() {
        lock.lock();
        try {
            unloading = true;
        } finally {
            lock.unlock();
        }
    }

    // Finish unloading phase
    public void finishUnloading() {
        lock.lock();
        try {
            unloading = false;
            canBoard.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // Read-only getter
    public int getCurrentLoad() {
        return currentLoad;
    }
}