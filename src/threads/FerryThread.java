package threads;

import model.Side;
import sync.SyncManager;
import core.WaitingQueue;
import sync.FerryControl;
import utils.Logger;
import utils.Statistics;
import java.util.concurrent.ThreadLocalRandom;

public class FerryThread extends Thread {
    private SyncManager syncManager;
    private Side currentSide;

    // ISSUE 13 FIX: Added volatile keyword [cite: 350, 351, 352]
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
                WaitingQueue queue = syncManager.getQueue(currentSide);

                Logger.ferryLoadingStarted(currentSide.name());
                ferryControl.waitForDeparture(queue);

                tripCount++;
                int currentLoad = ferryControl.getCurrentLoad();

                Logger.ferryDeparted(currentSide.name(), currentLoad, tripCount);
                Statistics.recordTrip(currentLoad);

                int travelTime = 800 + ThreadLocalRandom.current().nextInt(701);
                Thread.sleep(travelTime);

                currentSide = (currentSide == Side.A) ? Side.B : Side.A;

                // ISSUE 12 FIX: Real blocking unload gate [cite: 336, 337, 338, 339, 340, 341, 342, 343]
                ferryControl.signalArrival(); // Member A is adding this
                Logger.ferryArrived(currentSide.name(), tripCount);
                Logger.ferryUnloadingStarted(currentSide.name());

                ferryControl.startUnloading();
                ferryControl.setVehicleCount(ferryControl.getBoardingCount()); // Tell control how many
                ferryControl.waitForUnloadComplete(); // BLOCK until all exit
                ferryControl.finishUnloading();

                Logger.ferryUnloadingFinished(currentSide.name());

                ferryControl.resetAfterTrip();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Ferry thread was interrupted.");
        }
    }

    public void stopSimulation() {
        this.simulationRunning = false;
    }
}