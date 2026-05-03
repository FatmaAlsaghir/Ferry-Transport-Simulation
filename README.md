# Ferry Transport Simulation

Multithreaded ferry transport simulation using Java (Operating Systems project).

## Overview

This project models a ferry system transporting vehicles between two sides of a city.
Each vehicle is implemented as a thread, interacting with shared resources such as toll booths, queues, and the ferry.

The system focuses on synchronization, fairness, and efficient resource usage.

## Key Features

* Multithreaded design (vehicle threads and ferry thread)
* FIFO vehicle queues (no overtaking)
* Capacity-based ferry loading (maximum 20 units)
* Realistic departure conditions
* Randomized behavior (arrival, delays, starting side)
* Full round-trip simulation for all vehicles

## Concurrency Concepts

* Mutual exclusion (toll booths, ferry operations)
* Condition synchronization (boarding and unloading)
* Deadlock avoidance
* Starvation prevention (fair scheduling between sides)

## Technologies

* Java
* Multithreading (Locks, Conditions / synchronization)
* IntelliJ IDEA

## Output

The simulation logs key events such as:

* Vehicle arrival and queueing
* Ferry loading and departure
* Arrival and unloading

## Purpose

This project was developed as part of an Operating Systems course to demonstrate practical application of:

* Thread synchronization
* Resource allocation
* Concurrent system design

## Authors

Fatma Alsaghir, 
Ali Badalov, 
Ayşe Selin Kargı 
