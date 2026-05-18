package sync;

import core.WaitingQueue;
import model.Vehicle;
import utils.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FerryControl {

    private final Lock lock = new ReentrantLock(true); // fair lock

    private final Condition canBoard = lock.newCondition();
    private final Condition ferryReady = lock.newCondition();
    private final Condition arrivalCondition = lock.newCondition();
    private final Condition unloadDone = lock.newCondition(); // Issue 6

    private int currentLoad = 0;
    private final int MAX_CAPACITY = 20;

    private boolean loading = true;
    private boolean unloading = false;
    private boolean arrived = false;

    private int vehiclesToUnload = 0;                        // Issue 6
    private final Set<Vehicle> onFerry = new HashSet<>();    // Issue 6

    // Vehicle requests boarding — FIFO fixed (Issue 1)
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
            onFerry.add(vehicle);                            // Issue 6 — track boarded vehicles

            Logger.vehicleBoarded(vehicle.toString(), currentLoad);

            ferryReady.signal();

        } finally {
            lock.unlock();
        }
    }

    // canFit — queue parameter removed, null-check kept (Issue 1)
    private boolean canFit(Vehicle vehicle) {
        if (vehicle == null) return false;
        return currentLoad + vehicle.getSize() <= MAX_CAPACITY;
    }

    // Ferry waits until departure conditions are met — deadline + logging (Issue 6)
    public void waitForDeparture(WaitingQueue queue) throws InterruptedException {
        lock.lock();
        try {
            long deadline = System.nanoTime() + 3_000_000_000L;
            while (!shouldDepart(queue)) {
                long rem = deadline - System.nanoTime();
                if (rem <= 0) {
                    if (currentLoad > 0) {
                        Logger.ferryDepartureReason("timeout — starvation prevention");
                        break;
                    }
                    deadline = System.nanoTime() + 3_000_000_000L; // reset if still empty
                }
                ferryReady.awaitNanos(Math.max(rem, 1));
            }

            // Log departure reason
            if (currentLoad == MAX_CAPACITY)
                Logger.ferryDepartureReason("ferry full");
            else if (!queue.isEmpty() && queue.peek() != null && !canFit(queue.peek()))
                Logger.ferryDepartureReason("next vehicle does not fit");
            else if (queue.isEmpty() && currentLoad > 0)
                Logger.ferryDepartureReason("queue empty");

            loading = false;
        } finally {
            lock.unlock();
        }
    }

    // Departure conditions — empty-ferry guard + fixed logic (Issue 2)
    private boolean shouldDepart(WaitingQueue queue) {
        if (currentLoad == 0) return false;           // never depart empty
        if (currentLoad == MAX_CAPACITY) return true; // full
        if (queue.isEmpty()) return true;             // nobody left to board
        Vehicle next = queue.peek();
        return next != null && !canFit(next);         // next vehicle doesn't fit
    }

    // Vehicles wait until ferry arrives — per-vehicle tracking (Issue 6)
    public void waitForArrival(Vehicle vehicle) throws InterruptedException {
        lock.lock();
        try {
            while (!arrived || !onFerry.contains(vehicle))
                arrivalCondition.await();
            onFerry.remove(vehicle);
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

    // Set how many vehicles need to unload (Issue 6)
    public void setVehicleCount(int count) {
        lock.lock();
        try {
            vehiclesToUnload = count;
        } finally {
            lock.unlock();
        }
    }

    // Called by each vehicle after unloading (Issue 6)
    public void vehicleUnloaded() {
        lock.lock();
        try {
            if (--vehiclesToUnload <= 0) unloadDone.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // Ferry waits until all vehicles have unloaded (Issue 6)
    public void waitForUnloadComplete() throws InterruptedException {
        lock.lock();
        try {
            while (vehiclesToUnload > 0) unloadDone.await();
        } finally {
            lock.unlock();
        }
    }

    // Reset ferry for next trip — arrived reset first (Issue 3)
    public void resetAfterTrip() {
        lock.lock();
        try {
            arrived = false;      // MUST be first — prevents new waiters skipping wait
            currentLoad = 0;
            loading = true;
            unloading = false;
            vehiclesToUnload = 0; // Issue 6

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

    // getCurrentLoad — synchronized to prevent stale reads (Issue 4)
    public int getCurrentLoad() {
        lock.lock();
        try {
            return currentLoad;
        } finally {
            lock.unlock();
        }
    }
}