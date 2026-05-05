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
        VehicleThread[] vehicleThreads = new VehicleThread[30];

        for (int i = 0; i < 12; i++)
            vehicleThreads[id - 1] = new VehicleThread(new Vehicle(id++, VehicleType.CAR,     Side.A), syncManager);
        for (int i = 0; i < 10; i++)
            vehicleThreads[id - 1] = new VehicleThread(new Vehicle(id++, VehicleType.MINIBUS,  Side.A), syncManager);
        for (int i = 0; i < 8; i++)
            vehicleThreads[id - 1] = new VehicleThread(new Vehicle(id++, VehicleType.TRUCK,    Side.A), syncManager);

        // Ferry starts from a random side
        Side ferryStart = random.nextBoolean() ? Side.A : Side.B;
        FerryThread ferryThread = new FerryThread(syncManager, ferryStart);
        ferryThread.setDaemon(true);
        ferryThread.start();

        // Start all vehicle threads
        for (VehicleThread vt : vehicleThreads) {
            vt.start();
        }

        // Wait for all vehicles to complete round trip
        for (VehicleThread vt : vehicleThreads) {
            vt.join();
        }

        ferryThread.stopSimulation();
        Statistics.recordSimulationEnd();
        Logger.simulationEnded();
        Statistics.printReport();
    }
}