package model;

public class Vehicle {

    private final int id;
    private final VehicleType type;
    private Side currentSide;

    public Vehicle(int id, VehicleType type, Side startSide) {
        this.id = id;
        this.type = type;
        this.currentSide = startSide;
    }

    // Unique vehicle ID
    public int getId() {
        return id;
    }

    // Vehicle type (Car, Minibus, Truck)
    public VehicleType getType() {
        return type;
    }

    // Current side of the vehicle (A or B)
    public Side getCurrentSide() {
        return currentSide;
    }

    // Update vehicle side
    public void setCurrentSide(Side currentSide) {
        this.currentSide = currentSide;
    }

    // Switch side (A ↔ B) – helper method for cleaner thread logic
    public void switchSide() {
        currentSide = (currentSide == Side.A) ? Side.B : Side.A;
    }

    // Get size for capacity calculation
    public int getSize() {
        return type.getSize();
    }

    @Override
    public String toString() {
        return type + "-" + id;
    }
}