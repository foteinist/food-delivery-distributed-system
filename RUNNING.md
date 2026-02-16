# Running Guide – Distributed Food Delivery System

This document describes how to compile and run the distributed system locally.

---

## 1. Prerequisites

- Java JDK 17 or later
- Gson library (included in `libs/gson-2.8.9.jar`)

Verify Java installation:

java -version


---

## 2. Get Your Local IP Address

Windows:
ipconfig


Linux / macOS:
ifconfig


Use your **IPv4 address** (not localhost).

---

## 3. Compile the Project

From the root directory:

Windows:
javac -cp ".;libs/gson-2.8.9.jar" BackEnd/*.java


Linux / macOS:
javac -cp ".:libs/gson-2.8.9.jar" BackEnd/*.java


---

## 4. Start the System (IMPORTANT ORDER)

Each component must run in a separate terminal window.

### Step 1 – Reducer

java -cp "BackEnd;libs/gson-2.8.9.jar" Reducer <YOUR_IP> 5000


---

### Step 2 – Workers

Worker 1:
java -cp "BackEnd;libs/gson-2.8.9.jar" Worker 5001


Worker 2:
java -cp "BackEnd;libs/gson-2.8.9.jar" Worker 5002


---

### Step 3 – Master

java -cp "BackEnd;libs/gson-2.8.9.jar" Master <YOUR_IP> 5000 2


The last argument (`2`) indicates the number of workers.

---

### Step 4 – Manager Console (Optional)

java -cp "BackEnd;libs/gson-2.8.9.jar" Manager <YOUR_IP> 5000


---

### Step 5 – Client

java -cp "BackEnd;libs/gson-2.8.9.jar" Client <YOUR_IP> 5000


---

## 5. Android Client

The Android client allows mobile interaction with the system (search stores, place orders, rate stores).

### Prerequisites

- Android Studio 2022.2+
- Device or Emulator (Android 7.0/API 24+)
- Ensure backend nodes (Master, Workers, Reducer) are running

### Steps

1. Open the project in **Android Studio** (`app/` folder).  
2. Configure the backend IP in `Config.java`:

```java
public static final String MASTER_IP = "192.168.1.10";
public static final int MASTER_PORT = 5000;
```
3. Run the app on your device or emulator.
4. Ensure the device/emulator is on the same network as the backend.

---

## 6. Example Commands

### Manager Console

add-store BigStore/pizza.json
list-stores
sales-by-type pizzeria


### Client

search 37.9932963 23.733413 pizzeria 3 $
buy "Pizza Fun" margarita 2
rate "Pizza Fun" 5
exit


---

## 6. Port Configuration

| Component | Port |
|-----------|------|
| Reducer / Master | 5000 |
| Worker 1 | 5001 |
| Worker 2 | 5002 |

---

## 7. Troubleshooting

### Address already in use
Port is occupied. Kill the process or change port.

### Connection refused
- Ensure Reducer is started first
- Verify correct IP
- Check firewall settings

### ClassNotFoundException
Check classpath separator:
- `;` for Windows
- `:` for Linux/macOS

---

## 8. Important Notes

- Each component must run in a separate terminal.
- Data is stored in memory and is lost when workers stop.
- The system does not include fault tolerance or persistence.
