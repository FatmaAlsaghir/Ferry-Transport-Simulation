package core;

import model.Vehicle;

import java.util.LinkedList;
import java.util.Queue;

public class WaitingQueue {

    private final Queue<Vehicle> queue;

    public WaitingQueue() {
        this.queue = new LinkedList<>();
    }

    // Add vehicle to queue (FIFO)
    public synchronized void enqueue(Vehicle vehicle) {
        queue.add(vehicle);
    }

    // Remove vehicle from queue
    public synchronized Vehicle dequeue() {
        return queue.poll();
    }

    // Peek at next vehicle (without removing)
    public synchronized Vehicle peek() {
        return queue.peek();
    }

    // Check if queue is empty
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    // Get queue size
    public synchronized int size() {
        return queue.size();
    }
}