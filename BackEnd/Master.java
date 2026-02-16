
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Master node for distributed store inventory system.
 * Coordinates between multiple worker nodes to handle store operations.
 */
//eixa balei implements Runnable
public class Master implements Runnable {
    private static int port;  // Port master listens on for client connections
    private static int numWorkers;  // Count of active workers
    private  static String host;
    private ServerSocket providerSocket;  // Socket για ακρόαση

    private int connectedWorkers = 0;
    private static final Map<Integer, Socket> workers = new HashMap<>();  // Connected worker sockets
    private static final Map<Integer, List<Store>> workerStoreMap= new HashMap<>();
    private static final Map<Integer, ObjectOutputStream> outputStreams = new HashMap<>();
    private static final Map<Integer, ObjectInputStream> inputStreams = new HashMap<>();
    private static final Object commandLock = new Object();
    private boolean allWorkersReady = false;
    /**
     * Main entry point for the Master node.
     * @param args List of worker ports to connect to
     */
    public static void main(String[] args) throws IOException {
        // Validate arguments
        if (args.length < 3) {
            System.out.println("Usage: java Master <host> <port> <num_workers>");
            return;
        }

        host = args[0];  // π.χ. 192.168.1.24
        port = Integer.parseInt(args[1]);  // π.χ. 5000
        numWorkers = Integer.parseInt(args[2]);  // π.χ. 2


        System.out.println("Master is running on " + host + " : " + port);
        System.out.println("Starting Master with " + numWorkers + " workers.");

    
        Master master = new Master();
        master.connectAndDistributeStores();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nMaster is shutting down. Notifying workers...");
            
            // Στέλνει shutdown command (0) σε όλους τους Workers
            for (int workerId = 0; workerId < numWorkers; workerId++) {
                try {
                    ObjectOutputStream out = outputStreams.get(workerId);
                    if (out != null) {
                        out.writeObject(0);  // Κωδικός 0 = shutdown
                        out.flush();
                        System.out.println("Sent shutdown signal to worker " + workerId);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to notify worker " + workerId + ": " + e.getMessage());
                }
            }
        }));
        Thread masterThread = new Thread(master);
        masterThread.start();
    }

    public void run(){
        openServer();
    }

    public void openServer(){
        try{
            providerSocket=new ServerSocket(port,10);
            System.out.println("Listening for user connections...");

            while (true) {
                try {
                    Socket connection = providerSocket.accept();
                    System.out.println("New connection established with: " + connection.getInetAddress());
                    handleUserConnection(connection);

                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void handleUserConnection(Socket connection) {
        Thread userThread = new Thread(() -> {
            ObjectInputStream inputStream = null;
            ObjectOutputStream outputStream = null;
            System.out.println("Preparing to handle user connection...");

            try {
                inputStream = new ObjectInputStream(connection.getInputStream());
                outputStream = new ObjectOutputStream(connection.getOutputStream());

                // Wait until all workers are ready
                synchronized (commandLock) {
                    while (!allWorkersReady) {
                        try {
                            commandLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            System.err.println("Thread interrupted while waiting for workers");
                            return;
                        }
                    }
                }

                while (true) {
                    try {
                        System.out.println("----------------------------------------");
                        int choice = (int) inputStream.readObject();
                        String userType = (String) inputStream.readObject();
                        System.out.println("User connected. Type: " + userType + " | Choice: " + choice);

                        if ((userType.equals("manager") && choice == 9) || (userType.equals("client") && choice == 5)) {
                            System.out.println("Exit command received. Closing connection for " + userType);
                            break;
                        }
                        if (userType.equals("manager") && (choice == 1)) {
                            String obj = (String) inputStream.readObject();
                            String storeName = obj.split(",")[0];
                            int workerIndex = hashStoreToWorker(storeName);
                            System.out.println("Sending command to worker...");
                            Object response = sendCommandToWorker(workerIndex, choice, userType, obj);
                            System.out.println("Sending response to user...");
                            outputStream.writeObject(response);
                            outputStream.flush();
                        }
                        if ((userType.equals("manager") && (choice == 2 || choice == 3))) {
                            String obj = (String) inputStream.readObject();
                            String storeName = obj.split(",")[0];
                            int workerIndex = hashStoreToWorker(storeName);
                            System.out.println("Sending command to worker...");
                            Object response = sendCommandToWorker(workerIndex, choice, userType, obj);
                            System.out.println("Sending response to user...");
                            outputStream.writeObject(response);
                            outputStream.flush();
                            
                        }
                        if ((userType.equals("manager")) && choice == 4) {
                            String data = (String) inputStream.readObject();

                            System.out.println("Received Add Product to System request.");
                            System.out.println("Data received: " + data);

                            String[] parts = data.split(", Product ");
                            String storeName = parts[0].trim();
                            String[] productParts = parts[1].split(",");
                            String productName = productParts[0];
                            String productType = productParts[1];
                            int quantity = Integer.parseInt(productParts[2]);
                            double price = Double.parseDouble(productParts[3]);

                            System.out.println("Processing store: " + storeName);
                            int workerIndex = hashStoreToWorker(storeName);
                            String payload = storeName + "," + productName + "," + productType + "," + quantity + "," + price;
                            System.out.println("Sending command to worker " + workerIndex + " with payload: " + payload);
                            Object response = sendCommandToWorker(workerIndex, choice, userType, payload);
                            System.out.println("Received response from worker: " + response);
                            outputStream.writeObject(response);
                            outputStream.flush();

                        }
                        if ((userType.equals("manager")) && (choice == 5)) {
                            String data = (String) inputStream.readObject();
                            System.out.println("Received Remove Product from System request.");
                            String[] parts = data.split(", Product ");
                            String storeName = parts[0].trim();
                            String productName = parts[1].trim();
                            System.out.println("Processing store: " + storeName);
                            int workerIndex = hashStoreToWorker(storeName);
                            String payload = storeName + "," + productName;
                            System.out.println("Sending remove command to worker " + workerIndex + " with payload: " + payload);
                            Object response = sendCommandToWorker(workerIndex, choice, userType, payload);
                            System.out.println("Sending response to user...");
                            outputStream.writeObject(response);
                            outputStream.flush();
                    
                        }
                        if (choice == 6 && userType.equals("manager")) {
                            String storeName = (String) inputStream.readObject();
                            System.out.println("Received request for total sales per product in store: " + storeName);
                            int workerIndex = hashStoreToWorker(storeName);
                            Object response1 = sendCommandToWorker(workerIndex, choice, userType, storeName);
                            System.out.println("Sending response to user...");

                            Map<String, Integer> workerSales = new HashMap<>();

                            if (response1 instanceof Map) {
                                workerSales = (Map<String, Integer>) response1;
                                System.out.println("Sales received from worker " + workerSales);
                            }
                            StringBuilder sb = new StringBuilder();
                            sb.append("Store Sales: ").append(storeName).append("\n\n");

                            for (Map.Entry<String, Integer> entry : workerSales.entrySet()) {
                                sb.append("- ").append(entry.getKey())
                                        .append(": ").append(entry.getValue()).append(" sales\n");
                            }
                            System.out.println("Sending response to user...");
                            outputStream.writeObject(sb.toString());
                            outputStream.flush();
                            System.out.println("Result sent to manager: " + sb.toString());
                        }
                        if (choice == 7 && userType.equals("manager")) {
                            int subCategoryType = (int) inputStream.readObject();
                            System.out.println("Received subCategoryType: " + subCategoryType);
                            String categoryValue = (String) inputStream.readObject();
                            System.out.println("Received categoryValue: " + categoryValue);

                            Map<String, Integer> aggregatedSales = new HashMap<>();
                            int totalSales = 0;

                            for (int workerIndex = 0; workerIndex < numWorkers; workerIndex++) {
                                String payload = subCategoryType + "," + categoryValue;
                                Object response = sendCommandToWorker(workerIndex, choice, userType, payload);

                                System.out.println("Sending payload to worker " + workerIndex + ": " + payload);
                                System.out.println("Response from worker " + workerIndex + ": " + response);

                                if (response instanceof Map) {
                                    Map<String, Integer> workerSales = (Map<String, Integer>) response;
                                    System.out.println("Sales received from worker " + workerIndex + ": " + workerSales);

                                    for (Map.Entry<String, Integer> entry : workerSales.entrySet()) {
                                        aggregatedSales.merge(entry.getKey(), entry.getValue(), Integer::sum);
                                        totalSales += entry.getValue();
                                        System.out.println("Aggregated Sales after merging: " + aggregatedSales);
                                    }
                                } else {
                                    System.out.println("Invalid response from worker " + workerIndex);
                                }
                            }
                            StringBuilder result = new StringBuilder();
                            if (subCategoryType == 1) {
                                result.append("=== Total Sales for ").append("food Category").append(" category: ").append(categoryValue).append(" ===\n");

                            } else {
                                result.append("=== Total Sales for ").append("Product Category").append(" category: ").append(categoryValue).append(" ===\n");

                            }
                            for (Map.Entry<String, Integer> entry : aggregatedSales.entrySet()) {
                                result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                                System.out.println("Aggregated Sale: " + entry.getKey() + " = " + entry.getValue());

                            }

                            result.append("Total: ").append(totalSales).append("\n");
                            outputStream.writeObject(result.toString());
                            outputStream.flush();
                            System.out.println("Result sent to manager: " + result.toString());

                        }
                        if (choice == 8 && userType.equals("manager")) {
                            int answer = (int) inputStream.readObject();
                            System.out.println("Received answer: " + answer);
                            String data = (String) inputStream.readObject();
                            System.out.println("Received storesOrAll: " + data);

                            String[] parts = data.split(",");
                            if ("ALL".equalsIgnoreCase(data.trim())) {
                                System.out.println("Processing for all stores...");
                                Map<String, String> finalMap = new HashMap<>();

                                for (int workerIndex = 0; workerIndex < numWorkers; workerIndex++) {
                                    String payload = "ALL ," + answer;
                                    Object response = sendCommandToWorker(workerIndex, choice, userType, payload);
                                    if (response instanceof Map) {
                                        Map<String, String> responseMap = (Map<String, String>) response;
                                        synchronized (finalMap) {
                                            finalMap.putAll(responseMap);
                                        }
                                    }
                                }
                                StringBuilder finalResponseString = new StringBuilder();
                                for (Map.Entry<String, String> entry : finalMap.entrySet()) {
                                    finalResponseString.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                                }

                                String finalResponse = finalResponseString.toString();
                                outputStream.writeObject(finalResponse);
                                outputStream.flush();
                            } else {
                                System.out.println("Processing specific stores...");

                                StringBuilder combinedResponse = new StringBuilder();

                                String[] storeNames = parts[0].split(",");
                                for (String storeName : storeNames) {
                                    storeName = storeName.trim();
                                    System.out.println("Processing store: " + storeName);

                                    int workerIndex = hashStoreToWorker(storeName);
                                    String payload = storeName + "," + answer;
                                    System.out.println("Sending request to worker " + workerIndex + " with payload: " + payload);

                                    Object response = sendCommandToWorker(workerIndex, choice, userType, payload);
                                    if (response instanceof Map) {
                                        Map<String, String> responseMap = (Map<String, String>) response;
                                        for (Map.Entry<String, String> entry : responseMap.entrySet()) {
                                            combinedResponse.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                                        }
                                    }
                                }
                                String finalResponse = combinedResponse.toString();
                                System.out.println("Sending combined response to user: \n" + finalResponse);
                                outputStream.writeObject(finalResponse);
                                outputStream.flush();
                            }
                        }
                        if (userType.equals("client") && choice == 1) {
                            // Handle client request for nearby stores
                            System.out.println("Handling nearby stores request...");
                            String data = (String) inputStream.readObject();

                            System.out.println("Received coordinates data: " + data);

                            String[] parts = data.split(",");
                            double latitude = Double.parseDouble(parts[0]);
                            double longitude = Double.parseDouble(parts[1]);

                            // Create a list to store responses from all workers
                            List<String> allResponses = new ArrayList<>();

                            for (int i = 0; i < numWorkers; i++) {
                                System.out.println("Querying worker " + i + "...");
                                Object[] coords = new Object[]{latitude, longitude, 5.0};
                                Object response = sendClientCommandToWorker(i, choice, userType, coords);

                                if (response instanceof String) {
                                    System.out.println("Received response from worker " + i + ": " + response);
                                    allResponses.add((String) response);
                                }
                            }

                            // Combine all responses
                            StringBuilder combinedResponse = new StringBuilder();
                            for (String response : allResponses) {
                                if (!response.trim().isEmpty()) {
                                    combinedResponse.append(response).append("\n");
                                }
                            }

                            System.out.println("Sending combined response to client...");
                            outputStream.writeObject(combinedResponse.toString());
                            outputStream.flush();
                            
                        }
                        if (userType.equals("client") && choice == 2) {
                            // Μέσα στο ίδιο μπλοκ: else if (userType.equals("client") && choice == 2) {
                            Map<String, Object> filterCriteria = (Map<String, Object>) inputStream.readObject();
                            System.out.println("Received filter criteria from client: " + filterCriteria);

                            // -- MAP PHASE --
                            Map<String, List<Store>> mappedResults = mapPhase(filterCriteria);

                            // -- REDUCE PHASE --
                            List<Store> reducedResults = reducePhase(mappedResults, filterCriteria);

                            // Μετατροπή σε string για εμφάνιση στον client
                            List<String> formattedStores = reducedResults.stream()
                                    .map(store -> store.toString("client"))
                                    .collect(Collectors.toList());

                            // Αποστολή αποτελεσμάτων στον client
                            outputStream.writeObject(formattedStores);
                            outputStream.flush();
                            // Debug
                            System.out.println("\nFiltered Stores Results:");
                            formattedStores.forEach(System.out::println);
                        }
                        if (userType.equals("client") && choice == 3) {
                            String purchaseData = (String) inputStream.readObject();
                            String storeName = purchaseData.split("\\|")[0].trim();
                            int workerIndex = hashStoreToWorker(storeName);
                            System.out.println("Sending purchase command to worker...");
                            System.out.println("Order: "+ storeName);
                            Object response = sendClientCommandToWorker(workerIndex, choice, userType, purchaseData);
                            System.out.println("Response: "+ response);
                            System.out.println("Sending response to user...");
                            outputStream.writeObject(response);
                            outputStream.flush();
                            
                        }
                        if (userType.equals("client") && choice == 4) {
                            System.out.println("was here before dead Rating");
                            String obj = (String) inputStream.readObject();
                            String storeName = obj.split(",")[0];
                            int workerIndex = hashStoreToWorker(storeName);
                            System.out.println("Sending command to worker...");
                            System.out.println("Rating: "+ storeName);
                            Object response = sendClientCommandToWorker(workerIndex, choice, userType, obj);
                            System.out.println("Response: "+ response);
                            System.out.println("Sending response to user...");
                            outputStream.writeObject(response);
                            outputStream.flush();

                        }

                    } catch (EOFException eof) {
                        System.out.println("User closed connection.");
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Connection error: " + e.getMessage());
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                    connection.close();
                    System.out.println("User connection closed.");
                } catch (IOException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        });
        userThread.start();
    }

    private Double getDoubleFromFilter(Map<String, Object> filterCriteria, String key) {
        Object value = filterCriteria.get(key);
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        return null;
    }

    //-----------------------connectAndDistributeStores----------------------
    public void connectAndDistributeStores() throws IOException {
        StoreLoader loader = new StoreLoader();
        List<Store> stores = loader.loadStores("BigStore/BigStore.json");

        if (stores.isEmpty()) {
            System.out.println("No stores found.");
            return;
        }

        List<Integer> availableWorkers = new ArrayList<>();

        for (int i = 0; i < numWorkers; i++) {
            final int workerId = i;
            if (connectToWorker(workerId)) {
                availableWorkers.add(workerId);
            }
        }

        if (availableWorkers.isEmpty()) {
            System.out.println("No workers available. Exiting.");
            System.exit(1);
        }

        System.out.println("Connected to " + availableWorkers.size() + " workers.");
        System.out.println("Distributing " + stores.size() + " stores among them.");

        numWorkers=availableWorkers.size();
        for (Store store : stores) {
            int workerIndex = hashStoreToWorker(store.getStoreName());
            workerStoreMap.computeIfAbsent(workerIndex, k -> new ArrayList<>()).add(store);
        }

        for (int workerId = 0; workerId < numWorkers; workerId++){
            List<Store> assignedStores = workerStoreMap.getOrDefault(workerId, new ArrayList<>());

            try {
                ObjectOutputStream out = outputStreams.get(workerId);
                if (out != null) {
                    out.writeObject(assignedStores);
                    out.flush();
                    System.out.println("Sent " + assignedStores.size() + " stores to worker " + workerId);
                } else {
                    System.err.println("Output stream for worker " + workerId + " not found.");
                }
            } catch (IOException e) {
                System.err.println("Failed to send stores to worker " + workerId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("Stores sent to available workers.");
        allWorkersReady=true;
    }


    private int hashStoreToWorker(String storeName) {
        return Math.abs(storeName.hashCode()) % numWorkers;
    }

    private boolean connectToWorker(int workerId) {
        try {
            Socket socket = new Socket(host, port + workerId + 1);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            synchronized (commandLock) {
                workers.put(workerId, socket);
                outputStreams.put(workerId, out);
                inputStreams.put(workerId, in);
                connectedWorkers++;
                commandLock.notifyAll();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Worker " + workerId + " unavailable. Skipping.");
            return false;
        }
    }

    public Object sendCommandToWorker(int workerIndex, int choice, String userType,String obj) throws IOException, ClassNotFoundException {
        ObjectOutputStream out = outputStreams.get(workerIndex);
        ObjectInputStream in = inputStreams.get(workerIndex);

        synchronized (out) {
            out.writeObject(choice);
            out.writeObject(userType);
            out.writeObject(obj);
            out.flush();
        }

        synchronized (in) {
            return in.readObject();
        }
    }

    private Object sendClientCommandToWorker(int workerIndex, int command, String userType, Object payload) {
        try {
            ObjectOutputStream out = outputStreams.get(workerIndex);
            ObjectInputStream in = inputStreams.get(workerIndex);

            synchronized (out) {
                out.writeObject(command);
                out.writeObject(userType);
                out.writeObject(payload);
                out.flush();
            }

            synchronized (in) {
                return in.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR contacting worker";
        }
    }

    private Map<String, List<Store>> mapPhase(Map<String, Object> filterCriteria) {
        Map<String, List<Store>> results = new HashMap<>();

        for (int workerId = 0; workerId < numWorkers; workerId++) {
            try {
                System.out.println("Mapping with worker " + workerId + "...");
                Object result = sendClientCommandToWorker(workerId, 2, "client", filterCriteria);

                if (result instanceof List<?>) {
                    List<Store> workerStores = ((List<?>) result).stream()
                            .filter(obj -> obj instanceof Store)
                            .map(obj -> (Store) obj)
                            .collect(Collectors.toList());

                    results.put("worker_" + workerId, workerStores);
                }
            } catch (Exception e) {
                System.err.println("Error from worker " + workerId + ": " + e.getMessage());
                results.put("worker_" + workerId, new ArrayList<>());
            }
        }

        return results;
    }

    private List<Store> reducePhase(Map<String, List<Store>> mappedResults, Map<String, Object> filterCriteria) {
        List<Store> combined = new ArrayList<>();
        for (List<Store> stores : mappedResults.values()) {
            combined.addAll(stores);
        }

        String categoryFilter = (String) filterCriteria.get("category");
        Double starsFilter = getDoubleFromFilter(filterCriteria, "Stars");
        String priceLevelFilter = (String) filterCriteria.get("priceLevel");
        return combined.stream()
                .filter(store -> {
                    boolean matches = true;
                    if (categoryFilter != null && !store.getFoodCategory().equalsIgnoreCase(categoryFilter)) matches = false;
                    if (starsFilter != null && store.getStars() < starsFilter) matches = false;
                    if (priceLevelFilter != null && !store.getPriceCategory().equals(priceLevelFilter)) matches = false;
                    return matches;
                })
                .sorted((s1, s2) -> {
                    int cmp = Double.compare(s2.getStars(), s1.getStars());
                    if (cmp != 0) return cmp;
                    return Double.compare(s1.calculateAveragePrice(), s2.calculateAveragePrice());
                })
                .collect(Collectors.toList());
    }
}