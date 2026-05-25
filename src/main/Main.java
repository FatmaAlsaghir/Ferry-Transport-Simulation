package main;

import model.Side;
import model.Vehicle;
import model.VehicleType;
import sync.SyncManager;
import threads.FerryThread;
import threads.VehicleThread;
import utils.Logger;
import utils.Statistics;

import java.util.Random;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        Logger.startSimulation();

        Statistics.recordSimulationStart();

        SyncManager syncManager = new SyncManager();

        Random random = new Random();


        // 12 Cars, 10 Minibuses, 8 Trucks = 30 vehicles total
        // Starting side is passed as dummy — VehicleThread assigns randomly

        int id = 1;

        VehicleThread[] vehicleThreads =
                new VehicleThread[30];


        // Cars
        for (int i = 0; i < 12; i++) {

            vehicleThreads[id - 1] =
                    new VehicleThread(
                            new Vehicle(
                                    id++,
                                    VehicleType.CAR,
                                    Side.A
                            ),
                            syncManager
                    );
        }


        // Minibuses
        for (int i = 0; i < 10; i++) {

            vehicleThreads[id - 1] =
                    new VehicleThread(
                            new Vehicle(
                                    id++,
                                    VehicleType.MINIBUS,
                                    Side.A
                            ),
                            syncManager
                    );
        }


        // Trucks
        for (int i = 0; i < 8; i++) {

            vehicleThreads[id - 1] =
                    new VehicleThread(
                            new Vehicle(
                                    id++,
                                    VehicleType.TRUCK,
                                    Side.A
                            ),
                            syncManager
                    );
        }


        // Ferry starts randomly
        Side ferryStart =
                random.nextBoolean()
                        ? Side.A
                        : Side.B;

        Logger.log(
                "Ferry starts on Side "
                        + ferryStart.name()
        );


        FerryThread ferryThread =
                new FerryThread(
                        syncManager,
                        ferryStart
                );

        ferryThread.start();


        // Start vehicle threads
        for (VehicleThread vt : vehicleThreads) {

            vt.start();
        }


        // Wait for all vehicles
        for (VehicleThread vt : vehicleThreads) {

            vt.join();
        }

        // Record end time HERE — all vehicles are done, this is the true end
        Statistics.recordSimulationEnd();

        // Stop ferry thread cleanly
        ferryThread.stopSimulation();

        ferryThread.interrupt();


        // Final statistics
        Logger.simulationEnded();

        Statistics.printReport();
    }
}