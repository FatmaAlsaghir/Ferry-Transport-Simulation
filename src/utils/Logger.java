package utils;

import java.util.concurrent.atomic.AtomicLong;

public class Logger {

    // Simulation start time (milliseconds)
    private static long startTime = -1;

    // Thread-safe flag to avoid double-init
    private static final Object initLock = new Object();

    // Start the simulation clock
    public static void startSimulation() {
        synchronized (initLock) {
            if (startTime == -1) {
                startTime = System.currentTimeMillis();
                log("=== FERRY TRANSPORT SIMULATION STARTED ===");
            }
        }
    }

    // Get elapsed time in milliseconds since simulation start
    public static long getElapsedTime() {
        if (startTime == -1) return 0;
        return System.currentTimeMillis() - startTime;
    }

    // Core log method - prints [Time X] message
    public static void log(String message) {
        long time = getElapsedTime();
        System.out.printf("[Time %d] %s%n", time, message);
    }

    // ── Vehicle lifecycle events ──────────────────────────────────────

    public static void vehicleCreated(String vehicleName, String side) {
        log(vehicleName + " created on Side " + side);
    }

    public static void vehicleEnteredToll(String vehicleName, String tollName, String side) {
        log(vehicleName + " entered toll " + tollName + " on Side " + side);
    }

    public static void vehicleExitedToll(String vehicleName, String tollName, String side) {
        log(vehicleName + " exited toll " + tollName + " on Side " + side);
    }

    public static void vehicleJoinedQueue(String vehicleName, String side) {
        log(vehicleName + " joined queue on Side " + side);
    }

    public static void vehicleBoarded(String vehicleName, int currentLoad) {
        log("Ferry loaded " + vehicleName + " (current load = " + currentLoad + ")");
    }

    public static void vehicleUnloaded(String vehicleName, String side) {
        log(vehicleName + " unloaded on Side " + side);
    }

    public static void vehicleWaiting(String vehicleName, String side) {
        log(vehicleName + " is waiting before return trip on Side " + side);
    }

    public static void vehicleReturning(String vehicleName, String side) {
        log(vehicleName + " re-entered system for return trip from Side " + side);
    }

    public static void vehicleCompleted(String vehicleName) {
        log(vehicleName + " completed full round trip ✓");
    }

    // ── Ferry events ──────────────────────────────────────────────────

    public static void ferryDeparted(String fromSide, int load, int tripNumber) {
        log("Ferry departed from Side " + fromSide +
                " (load = " + load + ", trip #" + tripNumber + ")");
    }

    public static void ferryArrived(String toSide, int tripNumber) {
        log("Ferry arrived at Side " + toSide + " (trip #" + tripNumber + ")");
    }

    public static void ferryUnloadingStarted(String side) {
        log("Ferry started unloading on Side " + side);
    }

    public static void ferryUnloadingFinished(String side) {
        log("Ferry finished unloading on Side " + side + " — boarding may begin");
    }

    public static void ferryLoadingStarted(String side) {
        log("Ferry started loading on Side " + side);
    }

    public static void ferryDepartureReason(String reason) {
        log("Ferry departure triggered: " + reason);
    }

    // ── Simulation end ────────────────────────────────────────────────

    public static void simulationEnded() {
        log("=== SIMULATION ENDED ===");
    }
}
