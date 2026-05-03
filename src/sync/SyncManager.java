package sync;

import core.TollBooth;
import core.WaitingQueue;
import model.Side;

public class SyncManager {

    // Queues for both sides
    private final WaitingQueue queueA = new WaitingQueue();
    private final WaitingQueue queueB = new WaitingQueue();

    // Toll booths (2 per side)
    private final TollBooth tollA1 = new TollBooth("A1");
    private final TollBooth tollA2 = new TollBooth("A2");

    private final TollBooth tollB1 = new TollBooth("B1");
    private final TollBooth tollB2 = new TollBooth("B2");

    // Ferry control
    private final FerryControl ferryControl = new FerryControl();

    // Get queue based on side
    public WaitingQueue getQueue(Side side) {
        return (side == Side.A) ? queueA : queueB;
    }

    // Get toll booths based on side
    public TollBooth[] getTolls(Side side) {
        if (side == Side.A) {
            return new TollBooth[]{tollA1, tollA2};
        } else {
            return new TollBooth[]{tollB1, tollB2};
        }
    }

    // Get ferry control
    public FerryControl getFerryControl() {
        return ferryControl;
    }
}
