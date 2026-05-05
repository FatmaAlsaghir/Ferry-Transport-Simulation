package threads;

import core.WaitingQueue;
import model.Side;
import sync.FerryControl;
import sync.SyncManager;
import utils.Logger;
import utils.Statistics;

import java.util.concurrent.ThreadLocalRandom;

public class FerryThread extends Thread
{
    private SyncManager syncManager;
    private Side currentSide;
    private boolean simulationRunning = true;
    private int tripCount = 0;

    public FerryThread(SyncManager syncManager, Side startSide)
    {
        this.syncManager = syncManager;
        this.currentSide = startSide;
    }

    @Override
    public void run()
    {
        try
        {
            FerryControl ferryControl = syncManager.getFerryControl();

            while(simulationRunning)
            {
                WaitingQueue queue = syncManager.getQueue(currentSide);

                Logger.ferryLoadingStarted(currentSide.name());
                ferryControl.waitForDeparture(queue);

                tripCount++;
                int currentLoad = 0; //ferryControl.getCurrentLoad(); is being awaited to be implemented

                Logger.ferryDeparted(currentSide.name(), currentLoad, tripCount);
                Statistics.recordTrip(currentLoad);

                int tripDelay = 500 + ThreadLocalRandom.current().nextInt(1200);
                Thread.sleep(tripDelay);

                currentSide = (currentSide == Side.A) ? Side.B : Side.A;
                Logger.ferryArrived(currentSide.name(), tripCount);

                Logger.ferryUnloadingStarted(currentSide.name());
                ferryControl.startUnloading();

                int unloadDelay = 300 + ThreadLocalRandom.current().nextInt(700);
                Thread.sleep(unloadDelay);

                ferryControl.finishUnloading();
                Logger.ferryUnloadingFinished(currentSide.name());

                ferryControl.resetAfterTrip();
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            System.err.println("Ferry thread was interrupted.");
        }
    }

    public void stopSimulation()
    {
        this.simulationRunning = false;
    }
}
