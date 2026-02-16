#  Distributed Food Delivery System

A distributed food delivery platform built in **Java** as part of a Distributed Systems project.  
The system follows a **Master–Worker architecture** and demonstrates core distributed systems concepts such as task coordination, message passing, and parallel data processing.

---

##  Project Overview

This application simulates a real-world food delivery service where:

- A **Master Server** coordinates the system
- Multiple **Workers** process and manage store data
- A **Reducer** aggregates results
- A **Manager Console** handles store administration
- A **Client Application** allows users to search, order, and rate stores

The system communicates via **TCP sockets** and uses **Gson** for data serialization.

---

##  Architecture

```
Client / Manager
        |
        v
     Master
        |
   -------------
   |           |
 Worker       Worker
        |
      Reducer
```

### Core Components

| Component | Responsibility |
|-----------|----------------|
| **Master** | Coordinates requests and distributes work via hash function |
| **Workers** | Handle store data and customer interactions (in-memory storage) |
| **Reducer** | Aggregates and combines results from multiple workers |
| **Manager** | Admin console for store management (CLI) |
| **Client** | End-user interface (Android app) | 

---

##  Features

###  Manager Operations
| Feature | Description |
|---------|-------------|
|  Add/Remove Stores | Register new restaurants or remove existing ones |
|  Product Management | Upload and update product catalogs |
|  Sales Reports | View statistics and analytics |

###  Customer Operations
| Feature | Description |
|---------|-------------|
|  Smart Search | Find stores by location radius, category, rating, or price range |
|  Purchase | Order products from selected stores |
|  Rate | Leave ratings for stores (1-5 stars) |

---

##  Technologies Used

| Technology | Purpose |
|------------|---------|
| **Java (JDK 17+)** | Core programming language for backend |
| **TCP Sockets** | Low-level network communication between components |
| **Gson** | JSON serialization/deserialization for data exchange |
| **Multi-threading** | Concurrent request handling with synchronized blocks |
| **Android SDK** | Mobile client development |

---

##  How to Run

- For detailed compilation and execution instructions, please see:

#  [RUNNING.md](./RUNNING.md)

This file contains:
-  Compilation steps for different environments
-  Execution order of components (Reducer → Workers → Master → Manager/Client)
-  Port configuration (5000, 5001, 5002)
-  Example commands with expected output
-  Troubleshooting tips for common issues

### Quick Start (Windows)
```bash
# Clone the repository
git clone https://github.com/foteinist/food-delivery-distributed-system.git
cd food-delivery-distributed-system

# Run the batch script (interactive menu)
run-project.bat

---

##  Educational Purpose

- This project was developed for academic purposes to demonstrate:

| Concept | Implementation |
|---------|----------------|
|  **Distributed Coordination** | Master-Worker architecture with TCP socket communication |
|  **MapReduce Pattern** | Parallel data processing across multiple worker nodes |
|  **Network Programming** | Custom TCP protocol for inter-process communication |
|  **Concurrency** | Multi-threaded server handling multiple client requests |
|  **Synchronization** | Pure Java synchronization using `synchronized` and `wait-notify` |
|  **Data Distribution** | Hash-based store placement: `H(storeName) mod NumberOfNodes` |
|  **Mobile Development** | Android client with asynchronous network operations |
|  **Modular Design** | Separation of concerns between Master, Workers, and Clients |

---

##  Notes

- Data is stored **in memory**
- No persistence layer included
- Designed for learning & experimentation

---

##  Authors

- Konstantina Karapetsa
- Foteini Sotiropoulou
