package sync;

import core.WaitingQueue;
import model.Side;
import model.Vehicle;
import utils.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FerryControl {

    private final Lock lock = new ReentrantLock(true);

    private final Condition canBoard        = lock.newCondition();
    private final Condition ferryReady      = lock.newCondition();
    private final Condition arrivalCondition = lock.newCondition();
    private final Condition unloadDone      = lock.newCondition();

    private int currentLoad = 0;
    private final int MAX_CAPACITY = 20;

    private boolean loading   = true;
    private boolean unloading = false;
    private boolean arrived   = false;

    private Side arrivalSide;

    // track which queue the ferry is currently loading from.
    // null means the ferry is not in a loading phase.
    private WaitingQueue currentLoadingQueue = null;

    private int vehiclesToUnload = 0;

    private final Set<Vehicle> onFerry = new HashSet<>();


    // Vehicle requests boarding
    public void requestBoarding(
            Vehicle vehicle,
            WaitingQueue queue
    ) throws InterruptedException {

        lock.lock();

        try {

            // added `currentLoadingQueue != queue` as a guard.
            // Without this, vehicles from the wrong side woke on
            // canBoard.signalAll(), passed every other condition,
            // and boarded while the ferry was docked on the opposite
            // shore — this was the root cause of the global FIFO bug.
            while (!loading
                    || unloading
                    || currentLoadingQueue != queue
                    || !vehicle.equals(queue.peek())
                    || !canFit(vehicle)) {

                canBoard.await();
            }

            queue.dequeue();

            currentLoad += vehicle.getSize();

            onFerry.add(vehicle);

            Logger.vehicleBoarded(
                    vehicle.toString(),
                    currentLoad
            );

            ferryReady.signal();

        } finally {

            lock.unlock();
        }
    }


    // Check capacity
    private boolean canFit(Vehicle vehicle) {

        if (vehicle == null)
            return false;

        return currentLoad + vehicle.getSize() <= MAX_CAPACITY;
    }


    // Ferry waits for departure conditions
    public void waitForDeparture(
            WaitingQueue queue
    ) throws InterruptedException {

        lock.lock();

        try {

            // set the active loading queue so requestBoarding
            // knows which side's vehicles are allowed to board.
            // Then signal all so waiting vehicles re-evaluate their
            // condition immediately — correct-side vehicles unblock,
            // wrong-side vehicles go back to sleep.
            currentLoadingQueue = queue;
            canBoard.signalAll();

            long deadline =
                    System.nanoTime() + 3_000_000_000L;

            while (!shouldDepart(queue)) {

                long remaining = deadline - System.nanoTime();

                if (remaining <= 0) {

                    if (currentLoad > 0) {

                        Logger.ferryDepartureReason(
                                "timeout — starvation prevention"
                        );

                        break;
                    }

                    deadline =
                            System.nanoTime() + 3_000_000_000L;
                }

                ferryReady.awaitNanos(
                        Math.max(remaining, 1)
                );
            }

            // Departure reason logging
            if (currentLoad == MAX_CAPACITY) {

                Logger.ferryDepartureReason("ferry full");

            } else if (!queue.isEmpty()
                    && queue.peek() != null
                    && !canFit(queue.peek())) {

                Logger.ferryDepartureReason(
                        "next vehicle does not fit"
                );

            } else if (queue.isEmpty()
                    && currentLoad > 0
                    && currentLoad < MAX_CAPACITY) {

                Logger.ferryDepartureReason("queue empty");
            }

            loading = false;

        } finally {

            lock.unlock();
        }
    }


    // Departure conditions
    private boolean shouldDepart(WaitingQueue queue) {

        if (currentLoad == 0)
            return false;

        if (currentLoad == MAX_CAPACITY)
            return true;

        if (queue.isEmpty())
            return true;

        Vehicle next = queue.peek();

        return next != null && !canFit(next);
    }


    // Vehicle waits until ferry arrives at its destination
    public void waitForArrival(
            Vehicle vehicle
    ) throws InterruptedException {

        lock.lock();

        try {

            while (!arrived || !onFerry.contains(vehicle)) {

                arrivalCondition.await();
            }

        } finally {

            lock.unlock();
        }
    }


    // Ferry signals arrival on a specific side
    public void signalArrival(Side side) {

        lock.lock();

        try {

            arrived    = true;
            arrivalSide = side;

            arrivalCondition.signalAll();

        } finally {

            lock.unlock();
        }
    }


    // Vehicles read which side the ferry arrived at
    public Side getArrivalSide() {

        lock.lock();

        try {

            return arrivalSide;

        } finally {

            lock.unlock();
        }
    }


    // Remove a vehicle from the onboard set after it unloads
    public void removeFromFerry(Vehicle vehicle) {

        lock.lock();

        try {

            onFerry.remove(vehicle);

        } finally {

            lock.unlock();
        }
    }


    // replaces the two-call race condition
    // setVehicleCount(getBoardingCount()) + waitForUnloadComplete()
    // Snapshots the boarding count and waits atomically under the
    // same lock acquisition — no window for the count to change
    // between reading and waiting.
    public void snapshotBoardingCountAndWait()
            throws InterruptedException {

        lock.lock();

        try {

            vehiclesToUnload = onFerry.size();

            while (vehiclesToUnload > 0) {

                unloadDone.await();
            }

        } finally {

            lock.unlock();
        }
    }


    // Called by each vehicle thread after it finishes unloading
    public void vehicleUnloaded() {

        lock.lock();

        try {

            vehiclesToUnload--;

            if (vehiclesToUnload <= 0) {

                unloadDone.signalAll();
            }

        } finally {

            lock.unlock();
        }
    }


    // Start unloading — blocks boarding
    public void startUnloading() {

        lock.lock();

        try {

            unloading = true;

        } finally {

            lock.unlock();
        }
    }


    // Finish unloading — boarding can resume
    public void finishUnloading() {

        lock.lock();

        try {

            unloading = false;

            canBoard.signalAll();

        } finally {

            lock.unlock();
        }
    }


    // Reset state for the next trip
    public void resetAfterTrip() {

        lock.lock();

        try {

            arrived              = false;
            arrivalSide          = null;
            currentLoad          = 0;
            loading              = true;
            unloading            = false;
            vehiclesToUnload     = 0;
            currentLoadingQueue  = null; // FIX: cleared so no vehicle
            // can sneak in between trips

            canBoard.signalAll();

        } finally {

            lock.unlock();
        }
    }


    public int getCurrentLoad() {

        lock.lock();

        try {

            return currentLoad;

        } finally {

            lock.unlock();
        }
    }


    public int getBoardingCount() {

        lock.lock();

        try {

            return onFerry.size();

        } finally {

            lock.unlock();
        }
    }
}