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

    // Vehicle requests boarding — FIFO fixed
    public void requestBoarding(Vehicle vehicle, WaitingQueue queue) throws InterruptedException {
        lock.lock();
        try {
            while (!loading || unloading
                    || !vehicle.equals(queue.peek()) // must be head of queue
                    || !canFit(vehicle)) {           // must fit
                canBoard.await();
            }

            queue.dequeue();
            currentLoad += vehicle.getSize();

            Logger.vehicleBoarded(vehicle.toString(), currentLoad);

            ferryReady.signal();

        } finally {
            lock.unlock();
        }
    }

    // canFit — queue parameter removed
    private boolean canFit(Vehicle vehicle) {
        return currentLoad + vehicle.getSize() <= MAX_CAPACITY;
    }

    // Ferry waits until departure conditions are met
    public void waitForDeparture(WaitingQueue queue) throws InterruptedException {
        lock.lock();
        try {
            while (!shouldDepart(queue)) {
                ferryReady.awaitNanos(1_000_000_000);
            }
            loading = false;
        } finally {
            lock.unlock();
        }
    }

    // Departure conditions — updated to use new canFit signature
    private boolean shouldDepart(WaitingQueue queue) {
        if (currentLoad == MAX_CAPACITY) return true;

        if (queue.isEmpty()) return currentLoad > 0;

        Vehicle next = queue.peek();
        return next != null && !canFit(next);
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

            canBoard.signalAll();

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