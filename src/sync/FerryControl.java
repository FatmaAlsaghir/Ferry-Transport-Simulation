package sync;

import core.WaitingQueue;
import model.Vehicle;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FerryControl {

    private final Lock lock = new ReentrantLock();

    private final Condition canBoard = lock.newCondition();
    private final Condition ferryReady = lock.newCondition();

    private int currentLoad = 0;
    private final int MAX_CAPACITY = 20;

    private boolean loading = true;
    private boolean unloading = false;

    // Try to board vehicle
    public void requestBoarding(Vehicle vehicle, WaitingQueue queue) throws InterruptedException {
        lock.lock();
        try {
            while (!loading || !canFit(vehicle, queue)) {
                canBoard.await();
            }

            // Remove from queue and board
            queue.dequeue();
            currentLoad += vehicle.getSize();

            System.out.println(vehicle + " boarded. Current load = " + currentLoad);

            ferryReady.signal(); // notify ferry

        } finally {
            lock.unlock();
        }
    }

    // Check if vehicle fits in remaining capacity
    private boolean canFit(Vehicle vehicle, WaitingQueue queue) {
        if (vehicle == null) return false;
        return currentLoad + vehicle.getSize() <= MAX_CAPACITY;
    }

    // Called by ferry thread to wait until ready
    public void waitForDeparture(WaitingQueue queue) throws InterruptedException {
        lock.lock();
        try {
            while (!shouldDepart(queue)) {
                ferryReady.await();
            }
            loading = false;
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

    // Reset after trip
    public void resetAfterTrip() {
        lock.lock();
        try {
            currentLoad = 0;
            loading = true;
            canBoard.signalAll(); // allow boarding again
        } finally {
            lock.unlock();
        }
    }
}
