package threads;

import core.WaitingQueue;
import model.Vehicle;
import model.Side;
import sync.SyncManager;
import core.TollBooth;
import core.WaitingQueue;
import sync.FerryControl;
import utils.Logger;
import utils.Statistics;
import java.util.concurrent.ThreadLocalRandom;

public class VehicleThread extends Thread {
    private Vehicle vehicle;
    private SyncManager syncManager;
    private Side originalSide;
    private String vName;

    public VehicleThread(Vehicle vehicle, SyncManager syncManager) {
        this.vehicle = vehicle;
        this.syncManager = syncManager;

        // ISSUE 11 FIX: Correct the name format to "Car-1" [cite: 310, 311, 312, 313]
        String raw = vehicle.getType().name();
        this.vName = raw.charAt(0) + raw.substring(1).toLowerCase(java.util.Locale.ENGLISH) + "-" + vehicle.getId();

        this.originalSide = (Math.random() > 0.5) ? Side.A : Side.B;
        this.vehicle.setCurrentSide(this.originalSide);

        Logger.vehicleCreated(vName, originalSide.name());
    }

    @Override
    public void run() {
        try {
            boolean roundTripComplete = false;
            int tripsCompleted = 0;

            while (!roundTripComplete) {
                Side currentSide = vehicle.getCurrentSide();

                // ISSUE 7 FIX: Distribute vehicles across both toll booths [cite: 250, 251, 252, 253, 254]
                TollBooth[] tolls = syncManager.getTolls(currentSide);
                int boothIndex = vehicle.getId() % tolls.length;
                TollBooth chosenToll = tolls[boothIndex];
                String tollName = "Toll-" + (boothIndex + 1);

                Logger.vehicleEnteredToll(vName, tollName, currentSide.name());
                chosenToll.enter(vehicle);

                int tollDelay = 50 + ThreadLocalRandom.current().nextInt(200);
                Thread.sleep(tollDelay);

                chosenToll.exit(vehicle);
                Logger.vehicleExitedToll(vName, tollName, currentSide.name());

                // Queue & Boarding
                WaitingQueue queue = syncManager.getQueue(currentSide);
                Logger.vehicleJoinedQueue(vName, currentSide.name());
                long queueEntryTime = Statistics.recordQueueEntry();
                queue.enqueue(vehicle);

                FerryControl ferryControl = syncManager.getFerryControl();
                ferryControl.requestBoarding(vehicle, queue);
                Statistics.recordBoarding(queueEntryTime);

                // ISSUE 9 FIX: Pass specific vehicle to waitForArrival [cite: 285, 286, 287]
                ferryControl.waitForArrival(this.vehicle);

                Side otherSide = (currentSide == Side.A) ? Side.B : Side.A;
                vehicle.setCurrentSide(otherSide);
                tripsCompleted++;

                // ISSUE 10 FIX: Log unloaded FIRST, then signal ferry [cite: 296, 297, 298]
                Logger.vehicleUnloaded(vName, otherSide.name());
                ferryControl.vehicleUnloaded();

                // ISSUE 8 FIX: Safer round trip check [cite: 267, 270, 271, 272, 273, 274]
                if (tripsCompleted >= 2 && vehicle.getCurrentSide().equals(originalSide)) {
                    roundTripComplete = true;
                    Logger.vehicleCompleted(vName);
                    Statistics.recordCompletion(); // Member C is adding this
                } else {
                    Logger.vehicleWaiting(vName, otherSide.name());
                    int returnDelay = 300 + ThreadLocalRandom.current().nextInt(700);
                    Thread.sleep(returnDelay);
                    Logger.vehicleReturning(vName, otherSide.name());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Vehicle " + vName + " was interrupted.");
        }
    }
}