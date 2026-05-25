package threads;

import core.WaitingQueue;
import model.Vehicle;
import model.Side;
import sync.SyncManager;
import core.TollBooth;
import sync.FerryControl;
import utils.Logger;
import utils.Statistics;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class VehicleThread extends Thread {

    private Vehicle vehicle;
    private SyncManager syncManager;
    private Side originalSide;
    private String vName;

    public VehicleThread(Vehicle vehicle, SyncManager syncManager) {

        this.vehicle = vehicle;
        this.syncManager = syncManager;

        String raw = vehicle.getType().name();

        this.vName =
                raw.charAt(0)
                        + raw.substring(1).toLowerCase(Locale.ENGLISH)
                        + "-"
                        + vehicle.getId();

        this.originalSide =
                (Math.random() > 0.5) ? Side.A : Side.B;

        this.vehicle.setCurrentSide(originalSide);

        Logger.vehicleCreated(vName, originalSide.name());
    }

    @Override
    public void run() {

        try {

            boolean roundTripComplete = false;

            int tripsCompleted = 0;

            while (!roundTripComplete) {

                Side currentSide = vehicle.getCurrentSide();

                // select toll booth
                TollBooth[] tolls =
                        syncManager.getTolls(currentSide);

                int boothIndex =
                        vehicle.getId() % tolls.length;

                TollBooth chosenToll =
                        tolls[boothIndex];

                String tollName =
                        "Toll-" + (boothIndex + 1);


                // toll entry
                Logger.vehicleEnteredToll(
                        vName,
                        tollName,
                        currentSide.name()
                );

                chosenToll.enter(vehicle);

                int tollDelay =
                        50 + ThreadLocalRandom.current().nextInt(200);

                Thread.sleep(tollDelay);

                chosenToll.exit(vehicle);

                Logger.vehicleExitedToll(
                        vName,
                        tollName,
                        currentSide.name()
                );


                // queue
                WaitingQueue queue =
                        syncManager.getQueue(currentSide);

                Logger.vehicleJoinedQueue(
                        vName,
                        currentSide.name()
                );

                long queueEntryTime =
                        Statistics.recordQueueEntry();

                queue.enqueue(vehicle);


                // boarding
                FerryControl ferryControl =
                        syncManager.getFerryControl();

                ferryControl.requestBoarding(vehicle, queue);

                Statistics.recordBoarding(queueEntryTime);


                // wait for ferry arrival
                ferryControl.waitForArrival(vehicle);


                // determine new side ONLY after arrival
                Side newSide;

                if (currentSide == Side.A)
                    newSide = Side.B;
                else
                    newSide = Side.A;

                vehicle.setCurrentSide(newSide);

                tripsCompleted++;


                // unloading log
                Logger.vehicleUnloaded(
                        vName,
                        newSide.name()
                );


                // remove from ferry + notify unload complete
                ferryControl.removeFromFerry(vehicle);

                ferryControl.vehicleUnloaded();


                // round trip complete
                if (tripsCompleted >= 2
                        && vehicle.getCurrentSide().equals(originalSide)) {

                    roundTripComplete = true;

                    Logger.vehicleCompleted(vName);

                    Statistics.recordCompletion();

                } else {

                    Logger.vehicleWaiting(
                            vName,
                            newSide.name()
                    );

                    int returnDelay =
                            300 + ThreadLocalRandom.current().nextInt(700);

                    Thread.sleep(returnDelay);

                    Logger.vehicleReturning(
                            vName,
                            newSide.name()
                    );
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.err.println(
                    "Vehicle " + vName + " was interrupted."
            );
        }
    }
}