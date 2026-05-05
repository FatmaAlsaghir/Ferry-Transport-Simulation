package threads;

import core.TollBooth;
import core.WaitingQueue;
import model.Side;
import model.Vehicle;
import sync.FerryControl;
import sync.SyncManager;
import utils.Logger;
import utils.Statistics;

public class VehicleThread extends Thread
{
    private Vehicle vehicle;
    private SyncManager syncManager;
    private Side originalSide;
    private String vName;

    public VehicleThread(Vehicle vehicle, SyncManager syncManager)
    {
        this.vehicle = vehicle;
        this.syncManager = syncManager;
        this.vName = vehicle.getType() + "-" + vehicle.getId();

        this.originalSide = (Math.random() > 0.5) ? Side.A : Side.B;
        this.vehicle.setCurrentSide(this.originalSide);

        Logger.vehicleCreated(vName, originalSide.name());
    }

    @Override
    public void run()
    {
        try
        {
            boolean roundTripComplete = false;
            int tripsComplete = 0;

            while(!roundTripComplete)
            {
                Side currentSide = vehicle.getCurrentSide();
                TollBooth[] tolls = syncManager.getTolls(currentSide);
                TollBooth chosenToll = tolls[0];

                Logger.vehicleEnteredToll(vName, "Toll-1", currentSide.name());
                chosenToll.enter(this.vehicle);
                Thread.sleep(100);
                chosenToll.exit(this.vehicle);
                Logger.vehicleExitedToll(vName, "Toll-1", currentSide.name());

                WaitingQueue queue = syncManager.getQueue(currentSide);
                Logger.vehicleJoinedQueue(vName, currentSide.name());
                long queueEntryTime = Statistics.recordQueueEntry();
                queue.enqueue(this.vehicle);

                FerryControl ferryControl = syncManager.getFerryControl();
                ferryControl.requestBoarding(this.vehicle, queue);

                Statistics.recordBoarding(queueEntryTime);

                Side otherSide = (currentSide == Side.A) ? Side.B : Side.A;
                vehicle.setCurrentSide(otherSide);
                tripsComplete++;
                Logger.vehicleUnloaded(vName, otherSide.name());

                if(tripsComplete == 2 && vehicle.getCurrentSide() == originalSide)
                {
                    roundTripComplete = true;

                }
                else
                {
                    Logger.vehicleWaiting(vName, otherSide.name());
                    Thread.sleep(500);
                    Logger.vehicleReturning(vName, otherSide.name());
                }
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            System.err.println("Vehicle " + vehicle.getId() + " was interrupted.");
        }
    }
}
