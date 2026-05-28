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

    private final Vehicle vehicle;

    private final SyncManager syncManager;

    private final Side originalSide;

    private final String vName;

    public VehicleThread(
            Vehicle vehicle,
            SyncManager syncManager
    ) {

        this.vehicle     = vehicle;
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

            int tripsCompleted = 0;

            while (tripsCompleted < 2) {

                Side currentSide = vehicle.getCurrentSide();


                // --- Toll phase ---

                TollBooth[] tolls =
                        syncManager.getTolls(currentSide);

                // randomize booth selection instead of
                // deterministic vehicle.getId() % length, which
                // always sends even IDs to Toll-1 and odd to Toll-2
                int boothIndex =
                        ThreadLocalRandom.current()
                                .nextInt(tolls.length);

                TollBooth chosenToll = tolls[boothIndex];

                String tollName = "Toll-" + (boothIndex + 1);

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


                // --- Queue phase ---

                WaitingQueue queue =
                        syncManager.getQueue(currentSide);

                // enqueue the vehicle FIRST, then log and record
                // stats. Previously the log and wait timer fired before
                // the vehicle was actually in the queue, which caused
                // incorrect wait time measurements and misleading logs.
                queue.enqueue(vehicle);

                Logger.vehicleJoinedQueue(
                        vName,
                        currentSide.name()
                );

                long queueEntryTime =
                        Statistics.recordQueueEntry();


                // --- Boarding phase ---

                FerryControl ferryControl =
                        syncManager.getFerryControl();

                // Passes the correct side's queue — with the FerryControl
                // fix, this vehicle will block if the ferry is currently
                // loading the opposite side's queue
                ferryControl.requestBoarding(vehicle, queue);

                Statistics.recordBoarding(queueEntryTime);


                // --- Crossing phase ---

                // Block until the ferry arrives at the destination
                ferryControl.waitForArrival(vehicle);

                // Read the side the ferry actually arrived at
                Side newSide = ferryControl.getArrivalSide();

                vehicle.setCurrentSide(newSide);

                tripsCompleted++;


                // --- Unloading phase ---

                Logger.vehicleUnloaded(vName, newSide.name());

                ferryControl.removeFromFerry(vehicle);

                ferryControl.vehicleUnloaded();


                // --- Return or complete ---

                if (tripsCompleted >= 2) {

                    Logger.vehicleCompleted(vName);

                    Statistics.recordCompletion();

                } else {

                    // Wait on destination side before returning
                    Logger.vehicleWaiting(
                            vName,
                            newSide.name()
                    );

                    int returnDelay =
                            300 + ThreadLocalRandom.current()
                                    .nextInt(700);

                    Thread.sleep(returnDelay);

                    Logger.vehicleReturning(
                            vName,
                            newSide.name()
                    );

                    // Loop continues — vehicle re-enters from newSide
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