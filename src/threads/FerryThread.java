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

    private volatile boolean simulationRunning = true;

    private int tripCount = 0;

    public FerryThread(
            SyncManager syncManager,
            Side startSide
    ) {

        this.syncManager = syncManager;

        this.currentSide = startSide;
    }

    @Override
    public void run() {

        try {

            FerryControl ferryControl =
                    syncManager.getFerryControl();

            while (simulationRunning) {

                WaitingQueue queue =
                        syncManager.getQueue(currentSide);


                // loading phase
                Logger.ferryLoadingStarted(
                        currentSide.name()
                );

                ferryControl.waitForDeparture(queue);


                // departure
                tripCount++;

                int currentLoad =
                        ferryControl.getCurrentLoad();

                Logger.ferryDeparted(
                        currentSide.name(),
                        currentLoad,
                        tripCount
                );

                Statistics.recordTrip(currentLoad);


                // simulate travel
                int travelTime =
                        800 + ThreadLocalRandom.current().nextInt(701);

                Thread.sleep(travelTime);


                // move to opposite side
                currentSide =
                        (currentSide == Side.A)
                                ? Side.B
                                : Side.A;


                // arrival
                Logger.ferryArrived(
                        currentSide.name(),
                        tripCount
                );


                // allow vehicles to unload
                ferryControl.signalArrival();


                // unloading phase
                Logger.ferryUnloadingStarted(
                        currentSide.name()
                );

                ferryControl.startUnloading();


                // wait until all onboard vehicles unload
                ferryControl.setVehicleCount(
                        ferryControl.getBoardingCount()
                );

                ferryControl.waitForUnloadComplete();


                // unloading complete
                ferryControl.finishUnloading();

                Logger.ferryUnloadingFinished(
                        currentSide.name()
                );


                // prepare next trip
                ferryControl.resetAfterTrip();
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.err.println(
                    "Ferry thread was interrupted."
            );
        }
    }

    public void stopSimulation() {

        simulationRunning = false;
    }
}