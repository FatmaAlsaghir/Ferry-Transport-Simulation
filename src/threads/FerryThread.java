package threads;

import model.Side;
import sync.SyncManager;
import core.WaitingQueue;
import sync.FerryControl;
import utils.Logger;
import utils.Statistics;

import java.util.concurrent.ThreadLocalRandom;

public class FerryThread extends Thread {

    private final SyncManager syncManager;

    private Side currentSide;

    private volatile boolean simulationRunning = true;

    private int tripCount = 0;

    public FerryThread(SyncManager syncManager, Side startSide) {
        this.syncManager = syncManager;
        this.currentSide = startSide;
    }

    @Override
    public void run() {

        try {

            FerryControl ferryControl = syncManager.getFerryControl();

            while (simulationRunning) {

                // Must return a DIFFERENT WaitingQueue instance per side.
                // This is the fix point — if SyncManager returns one shared
                // queue for both sides, the global FIFO bug lives there.
                WaitingQueue queue = syncManager.getQueue(currentSide);

                // Announce ferry is open for boarding on this side
                Logger.ferryLoadingStarted(currentSide.name());

                // Blocks until departure condition is met.
                // Must load ONLY from the passed queue — not from an
                // internal global reference inside FerryControl.
                ferryControl.waitForDeparture(queue);

                // Departure
                tripCount++;
                int currentLoad = ferryControl.getCurrentLoad();

                Logger.ferryDeparted(
                        currentSide.name(),
                        currentLoad,
                        tripCount
                );

                Statistics.recordTrip(currentLoad);

                // Simulate crossing
                int travelTime =
                        800 + ThreadLocalRandom.current().nextInt(701);

                Thread.sleep(travelTime);

                // Switch to opposite side
                currentSide = (currentSide == Side.A) ? Side.B : Side.A;

                Logger.ferryArrived(currentSide.name(), tripCount);

                // Signal which side ferry has arrived at so waiting
                // vehicle threads can react correctly
                ferryControl.signalArrival(currentSide);

                // Unloading phase — no boarding allowed during this
                Logger.ferryUnloadingStarted(currentSide.name());
                ferryControl.startUnloading();

                // replaced the two-call race condition:
                // ferryControl.setVehicleCount(ferryControl.getBoardingCount())
                // ferryControl.waitForUnloadComplete()
                // with a single atomic method that snapshots the boarding
                // count and then waits — no window for a count change
                // between the two calls.
                // You must add this method to FerryControl.
                ferryControl.snapshotBoardingCountAndWait();

                ferryControl.finishUnloading();

                Logger.ferryUnloadingFinished(currentSide.name());

                // Reset load counter and boarding state for next trip
                ferryControl.resetAfterTrip();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Only report if this was an unexpected interrupt,
            // not a clean shutdown triggered by stopSimulation()
            if (simulationRunning) {
                System.err.println("Ferry thread interrupted unexpectedly.");
            }
        }
    }

    public void stopSimulation() {
        simulationRunning = false;
        // interrupt so the ferry wakes immediately if it is blocked
        // inside waitForDeparture or Thread.sleep — without this,
        // stopSimulation() sets the flag but the thread never sees it
        // and hangs until the next natural wake-up
        this.interrupt();
    }
}