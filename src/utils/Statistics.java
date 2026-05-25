package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {

    // ── Trip counter ──────────────────────────────────────────────────
    private static final AtomicInteger tripCount =
            new AtomicInteger(0);

    // ── Ferry load per trip ──────────────────────────────────────────
    private static final List<Integer> loadsPerTrip =
            Collections.synchronizedList(new ArrayList<>());

    private static final int FERRY_MAX_CAPACITY = 20;

    // ── Vehicle waiting times ────────────────────────────────────────
    // Waiting time = queue entry → boarding
    private static final List<Long> waitingTimes =
            Collections.synchronizedList(new ArrayList<>());

    // ── Simulation timing ────────────────────────────────────────────
    private static long simulationStartTime = -1;

    private static long simulationEndTime = -1;

    // ── Completed vehicles ───────────────────────────────────────────
    private static final AtomicInteger completedCount =
            new AtomicInteger(0);


    // Record completed vehicle
    public static void recordCompletion() {

        completedCount.incrementAndGet();
    }


    // Get completed vehicle count
    public static int getCompletedCount() {

        return completedCount.get();
    }


    // Simulation start
    public static void recordSimulationStart() {

        simulationStartTime =
                System.currentTimeMillis();
    }


    // Simulation end
    public static void recordSimulationEnd() {

        simulationEndTime =
                System.currentTimeMillis();
    }


    // Vehicle joins queue
    public static long recordQueueEntry() {

        return System.currentTimeMillis();
    }


    // Vehicle boards ferry
    public static void recordBoarding(
            long queueEntryTime
    ) {

        long waitMs =
                System.currentTimeMillis()
                        - queueEntryTime;

        waitingTimes.add(waitMs);
    }


    // Ferry departs
    public static void recordTrip(
            int loadUnits
    ) {

        tripCount.incrementAndGet();

        loadsPerTrip.add(loadUnits);
    }


    // ── Final Report ─────────────────────────────────────────────────

    public static void printReport() {

        System.out.println();

        System.out.println(
                "╔══════════════════════════════════════════╗"
        );

        System.out.println(
                "║         SIMULATION STATISTICS           ║"
        );

        System.out.println(
                "╚══════════════════════════════════════════╝"
        );


        // Total simulation time
        long totalMs =
                (simulationEndTime > 0
                        && simulationStartTime > 0)

                        ? simulationEndTime
                          - simulationStartTime

                        : 0;

        System.out.printf(
                "  Total simulation time   : %d ms (%.2f s)%n",
                totalMs,
                totalMs / 1000.0
        );


        // Number of trips
        int trips =
                tripCount.get();

        System.out.printf(
                "  Number of ferry trips   : %d%n",
                trips
        );


        // Waiting time statistics
        if (!waitingTimes.isEmpty()) {

            long sum = 0;

            long max = Long.MIN_VALUE;

            for (long w : waitingTimes) {

                sum += w;

                if (w > max)
                    max = w;
            }

            double avg =
                    (double) sum / waitingTimes.size();

            System.out.printf(
                    "  Average waiting time    : %.1f ms%n",
                    avg
            );

            System.out.printf(
                    "  Maximum waiting time    : %d ms%n",
                    max
            );

        } else {

            System.out.println(
                    "  Average waiting time    : N/A"
            );

            System.out.println(
                    "  Maximum waiting time    : N/A"
            );
        }


        // Ferry utilization
        if (!loadsPerTrip.isEmpty()) {

            int totalLoad = 0;

            for (int load : loadsPerTrip) {

                totalLoad += load;
            }

            double utilization =
                    (double) totalLoad
                            / (loadsPerTrip.size()
                            * FERRY_MAX_CAPACITY)
                            * 100.0;

            System.out.printf(
                    "  Ferry utilization ratio : %.1f%%%n",
                    utilization
            );

        } else {

            System.out.println(
                    "  Ferry utilization ratio : N/A"
            );
        }


        // Completed vehicles
        System.out.printf(
                "  Vehicles completed      : %d%n",
                getCompletedCount()
        );

        System.out.println(
                "══════════════════════════════════════════"
        );
    }
}