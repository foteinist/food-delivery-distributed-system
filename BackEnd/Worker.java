import java.io.*;
import java.net.*;
import java.util.*;
import static java.lang.Math.*;

public class Worker implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private List<Store> stores = new ArrayList<>();
    private final Object lock = new Object();
    private boolean isBeingUpdated=false;

    public Worker(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Worker <port>");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number.");
            return;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Worker waiting for connection on port " + port + "...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connection established with Master.");
                Worker worker = new Worker(socket);
                new Thread(worker).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   
    @Override
    public void run() {
        try {
            synchronized (lock) {
                if (stores.isEmpty()) {
                    Object receivedObject = in.readObject();
                    System.out.println("Received object type: " + receivedObject.getClass().getName());
                    if (receivedObject instanceof List) {
                        stores = (List<Store>) receivedObject;
                        System.out.println("Stores received from Master.");
                        lock.notifyAll();
                    } else {
                        System.err.println("Expected a list of stores, but received something else.");
                        out.writeObject("ERROR: Invalid data received.");
                        out.flush();
                        return;
                    }
                }
                else {
                    while (stores.isEmpty()) {
                        try {
                            lock.wait(50000);
                            if (stores.isEmpty()) {
                                System.err.println("Timeout while waiting for stores");
                                return;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }

            while (true) {
                Object commandObj = in.readObject();    // Integer (π.χ. 1 για ADD_STORE)

                if (commandObj instanceof Integer && (Integer) commandObj == 0) {
                    System.out.println("Received shutdown command from Master. Terminating...");
                    break;  // Exit the loop and terminate
                }
        
                Object identity = in.readObject();      // "manager" ή "client
                Object payload = in.readObject();       // Συνήθως String ή κάποιο composite string

                if (!(identity instanceof String) || (!((String) identity).equalsIgnoreCase("manager") && !((String) identity).equalsIgnoreCase("client"))) {
                    out.writeObject("UNAUTHORIZED_ACCESS");
                    out.flush();
                    continue;
                }

                if (!(commandObj instanceof Integer)) {
                    out.writeObject("INVALID_COMMAND");
                    out.flush();
                    continue;
                }

                int code = (Integer) commandObj;
                String result;
                String job = (String) identity;
                synchronized (lock) {
                    if (job.equals("manager")) {
                        switch (code) {
                            case 1: // ADD_STORE
                                String folderName = (String) payload;
                                result = handleAddStore(folderName);
                                out.writeObject(result);
                                out.flush();
                                lock.notifyAll();
                                break;
                            case 2: // ADD_AVAILABLE_PRODUCTS
                                result = handleAddAvailableProducts((String) payload);
                                out.writeObject(result);
                                out.flush();
                                lock.notifyAll();
                                break;
                            case 3: // REMOVE_AVAILABLE_PRODUCTS
                                result = handleRemoveAvailableProducts((String) payload);
                                out.writeObject(result);
                                out.flush();
                                lock.notifyAll();
                                break;
                            case 4: // ADD_PRODUCTS_IN_SYSTEM
                                result = handleAddProductInSystem((String) payload);
                                out.writeObject(result);
                                out.flush();
                                lock.notifyAll();
                                break;
                            case 5: // REMOVE_PRODUCTS_IN_SYSTEM
                                result = handleRemoveProductFromSystem((String) payload);
                                out.writeObject(result);
                                out.flush();
                                lock.notifyAll();
                                break;
                            case 6: // VIEW_TOTAL_SALES_BY_PRODUCT
                                Map<String, Integer> resultsPS = handleViewSalesByProductForStore((String) payload);
                                out.writeObject(resultsPS);
                                out.flush();
                                break;
                            case 7: // VIEW_TOTAL_SALES_BY_CATEGORY FOOD/PRODUCT
                                Map<String, Integer> results = handleViewTotalSalesByCategory((String) payload);
                                out.writeObject(results);
                                out.flush();
                                break;
                            case 8: // VIEW_PRICE_CATEGORY
                                Map<String, String> resultsP = handleViewPriceCategory((String) payload);
                                out.writeObject(resultsP);
                                out.flush();
                                break;
                            default: // EXIT
                                out.writeObject("exit");
                                out.flush();
                                lock.notifyAll();
                                break;
                        }
                    }
                    else if(job.equals("client")) {
                        System.out.println("Client connection...");
                        switch (code) {
                            case 1:
                                System.out.println("Handling GET_NEARBY_STORES command...");
                                if (payload instanceof Object[]) {
                                    Object[] coords = (Object[]) payload;

                                    System.out.println("Received coordinates: lat=" + coords[0] + ", lon=" + coords[1] + ", radius=" + coords[2]);

                                    List<Store> nearbyStores = getNearbyStores(
                                            (double) coords[0],
                                            (double) coords[1],
                                            (double) coords[2]
                                    );

                                    System.out.println("Found " + nearbyStores.size() + " nearby stores.");

                                    String formattedResponse = formatStoresResponse(
                                            nearbyStores,
                                            (double) coords[0],
                                            (double) coords[1]
                                    );
                                    System.out.println("Formatted response: \n" + formattedResponse);

                                    out.writeObject(formattedResponse);
                                    out.flush();
                                } else {
                                    out.writeObject("INVALID_COORDINATES");
                                    out.flush();
                                }
                                break;
                            case 2:
                                System.out.println("Handling FILTER_STORES_BY_PREFERENCES command...");
                                if (!(payload instanceof Map)) {
                                    out.writeObject("INVALID_FILTERS");
                                    out.flush();
                                    break;
                                }
                                Map<String, Object> filters = (Map<String, Object>) payload;
                                String filterCategory = (String) filters.get("category");
                                Double stars = null;
                                if (filters.get("Stars") instanceof Integer) {
                                    stars = ((Integer) filters.get("Stars")).doubleValue();
                                } else if (filters.get("Stars") instanceof Double) {
                                    stars = (Double) filters.get("Stars");
                                }
                                String filterPriceLevel = (String) filters.get("priceLevel");
                                System.out.println("Filters received:");
                                System.out.println("Category: " + filterCategory);
                                System.out.println("Stars: " + stars);
                                System.out.println("Price Level: " + filterPriceLevel);

                                List<Store> matchingStores = new ArrayList<>();
                                synchronized (stores) {
                                    for (Store store : stores) {
                                        double avgPrice = store.calculateAveragePrice();
                                        String priceCategory = calculatePriceCategory(avgPrice);
                                        boolean matches = true;

                                        if (filterCategory != null && !store.getFoodCategory().equalsIgnoreCase(filterCategory)) {
                                            matches = false;
                                        }

                                        if (stars != null && store.getStars() < stars) {
                                            matches = false;
                                        }

                                        if (filterPriceLevel != null && !priceCategory.equals(filterPriceLevel)) {
                                            matches = false;
                                        }

                                        if (matches) {
                                            matchingStores.add(store);
                                        }
                                    }
                                }
                                System.out.println("Found " + matchingStores.size() + " stores matching filters.");
                                out.writeObject(matchingStores);
                                out.flush();
                                break;
                            case 3:
                                System.out.println("Handling PURCHASE_PRODUCT command...");
                                if (!(payload instanceof String)) {
                                    out.writeObject("INVALID_PURCHASE_DATA");
                                    out.flush();
                                    break;
                                }

                                String purchaseDataStr = (String) payload;
                                String purchaseResult= purchaseProducts(purchaseDataStr);
                                System.out.println("purchaseResult: "+purchaseDataStr);
                                out.writeObject(purchaseResult);
                                out.flush();
                                break;
                            case 4:
                                System.out.println("Handling RATE_STORE command...");
                                if (!(payload instanceof String)) {
                                    out.writeObject("INVALID_RATING_DATA");
                                    out.flush();
                                    break;
                                }

                                String response = handleStoreRating((String) payload);
                                System.out.println("Response: "+ response);
                                out.writeObject(response);
                                out.flush();
                                break;
                            default:
                                out.writeObject("UNKNOWN_CLIENT_COMMAND");
                                out.flush();
                                break;
                        }
                    } else {
                        out.writeObject("INVALID_CLIENT_COMMAND");
                        out.flush();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Worker disconnected or error occurred.");
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
                System.out.println("Worker resources closed.");
            } catch (IOException e) {
                System.err.println("Error while closing resources: " + e.getMessage());
            }
        }
    }
    // ========== Client Command Handlers ==========

    private Store getStoreDetails(String storeName) {
        synchronized (stores) {
            return stores.stream()
                    .filter(s -> s.getStoreName().equals(storeName))
                    .findFirst()
                    .orElse(null);
        }
    }

    private List<Store> getNearbyStores(double latitude, double longitude, double radiusKm) {
        List<Store> nearbyStores = new ArrayList<>();
        synchronized (stores) {
            for (Store store : stores) {
                double distance = calculateDistance(latitude, longitude,
                        store.getLatitude(), store.getLongitude());
                if (distance <= radiusKm) {
                    nearbyStores.add(store);
                }
            }
        }
        return nearbyStores;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km

        double latDistance = toRadians(lat2 - lat1);
        double lonDistance = toRadians(lon2 - lon1);
        double a = sin(latDistance / 2) * sin(latDistance / 2)
                + cos(toRadians(lat1)) * cos(toRadians(lat2))
                * sin(lonDistance / 2) * sin(lonDistance / 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));

        return R * c;
    }

    private String formatStoresResponse(List<Store> stores, double clientLat, double clientLon) {
        if (stores.isEmpty()) return "No stores found within 5 km radius.";

        StringBuilder response = new StringBuilder("=== Stores within 5 km radius ===\n");
        for (Store store : stores) {
            double distance = calculateDistance(clientLat, clientLon, store.getLatitude(), store.getLongitude());
            response.append(String.format("\n» %s (%.2f km)\nCategory: %s\nRating: %d/5 (%d votes)\nLocation: %.6f, %.6f\n",
                    store.getStoreName(),
                    distance,
                    store.getFoodCategory(),
                    store.getStars(),
                    store.getNoOfVotes(),
                    store.getLatitude(),
                    store.getLongitude()
            ));

            if (!store.getProducts().isEmpty()) {
                response.append("  Available Products:\n");
                for (Product product : store.getProducts()) {
                    if (product.getAvailableAmount() > 0) {
                        response.append(String.format("    - %s: $%.2f (%d in stock)\n",
                                product.getProductName(),
                                product.getPrice(),
                                product.getAvailableAmount()));
                    }
                }
            }
        }
        return response.toString();
    }

    private String calculatePriceCategory(double avgPrice) {
        if (avgPrice <= 5.0) return "$";
        else if (avgPrice <= 15.0) return "$$";
        else return "$$$";
    }

    private String purchaseProducts(String data) {
        synchronized (lock) {
            setBeingUpdated(true);
            try {
                String[] parts = data.split("\\|");
                if (parts.length != 2) return "WRONG FORMAT. Use StoreName|product:quantity,product:quantity";

                String storeName = parts[0].trim();
                String[] itemTokens = parts[1].split(",");

                Store targetStore = null;
                for (Store store : stores) {
                    if (store.getStoreName().equalsIgnoreCase(storeName)) {
                        targetStore = store;
                        break;
                    }
                }

                if (targetStore == null) return "STORE NOT FOUND";

                Map<Product, Integer> productsToBuy = new HashMap<>();

                for (String item : itemTokens) {
                    String[] itemParts = item.split(":");
                    if (itemParts.length != 2) return "INVALID PRODUCT FORMAT";

                    String productName = itemParts[0].trim();
                    int quantity;
                    try {
                        quantity = Integer.parseInt(itemParts[1].trim());
                    } catch (NumberFormatException e) {
                        return "INVALID QUANTITY FOR: " + productName;
                    }

                    Product product = targetStore.getProductByName(productName);
                    if (product == null) return "PRODUCT NOT FOUND: " + productName;
                    if (product.getAvailableAmount() < quantity)
                        return "NOT ENOUGH STOCK FOR: " + productName;

                    productsToBuy.put(product, quantity);
                }

                for (Map.Entry<Product, Integer> entry : productsToBuy.entrySet()) {
                    entry.getKey().reduceStock(entry.getValue());
                }

                return "SUCCESS: Bought " + productsToBuy.size() + " items from " + storeName;
            } finally {
                setBeingUpdated(false);
            }
        }
    }


    public String handleStoreRating(String data) {
        synchronized (lock) {
            setBeingUpdated(true);
            try {
                System.out.println("Handling RATE_STORE command...");

                String[] parts = data.split(",", 2);
                if (parts.length != 2) {
                    return "MALFORMED_RATING_DATA";
                }

                String storeName = parts[0].trim();
                int rating;
                try {
                    rating = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    return "INVALID_RATING_VALUE";
                }

                if (rating < 1 || rating > 5) {
                    return "Rating must be between 1 and 5.";
                }

                for (Store store : stores) {
                    if (store.getStoreName().equalsIgnoreCase(storeName)) {
                        System.out.println("Before rating: " + store.toString("client"));
                        store.rateStore(rating);
                        System.out.println("After rating: " + store.toString("client"));
                        return "Store rated successfully.";
                    }
                }
                return "Store not found.";
            }finally {
                setBeingUpdated(false);
            }
        }
    }

    // ========== Manager Command Handlers ==========

    private void setBeingUpdated(boolean status) {
        synchronized (lock) {
            isBeingUpdated = status;
            if (!status) {
                lock.notifyAll();
            }
        }
    }
    //1
    private String handleAddStore(String jsonFilePath) {
        synchronized (lock) {
            setBeingUpdated(true);
            String answer=" ";
            StoreLoader loader = new StoreLoader();
            try {
                Store store = loader.readStoreFromFile(jsonFilePath);
                System.out.println("Store: " + store.toString("manager"));
                for (Store s : stores) {
                    synchronized (s){
                        if (s.getStoreName().equals(store.getStoreName())){
                            System.out.println("already in the system");
                            return " already in the system";
                        }
                    }
                }
                answer=store.toString("manager");
                stores.add(store);
                return answer;
            } catch (IOException e) {
                e.printStackTrace();
                return "false";
            }
            finally {
                setBeingUpdated(false);
            }
        }
    }

    //2
    private String handleAddAvailableProducts(String data) {
        synchronized (lock) {
            setBeingUpdated(true);
            try {
                String[] parts = data.split(",", 3);
                if (parts.length != 3) return "FAIL (wrong format)";

                String storeName = parts[0];
                String productName = parts[1];
                int quantity;

                try {
                    quantity = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    return "FAIL (invalid quantity)";
                }
                for (Store s : stores) {
                    if (s.getStoreName().equalsIgnoreCase(storeName)) {
                        System.out.println(s.toString("manager"));
                        Product p = s.getProductByName(productName);
                        String str = "Before: " + p.toString();
                        if (p != null) {
                            p.addStock(quantity);
                            System.out.println("--------------------product choice 2: " + p.toString());
                            str = "\nAfter: " + p.toString();
                            return str;
                        } else {
                            return "FAIL (product not found)";
                        }
                    }
                }
                return "FAIL (store not found)";
            }
            finally {
                setBeingUpdated(false);
            }
        }
    }

    //3
    private String handleRemoveAvailableProducts(String data) {
        synchronized (lock) {
            setBeingUpdated(true);
            try
            {
                String[] parts = data.split(",", 3);
                if (parts.length != 3) return "FAIL (wrong format)";

                String storeName = parts[0];
                String productName = parts[1];
                int quantity;

                try {
                    quantity = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    return "FAIL (invalid quantity)";
                }
                for (Store s : stores) {
                    if (s.getStoreName().equalsIgnoreCase(storeName)) {
                        System.out.println("---------------------------------Store choice 3: " + s.toString("manager"));
                        Product p = s.getProductByName(productName);
                        String str = "Before: " + p.toString();
                        System.out.println("product choice 3: " + p.toString());
                        if (p != null) {
                            if (p.getAvailableAmount() >= quantity && p.isAvailable()) {
                                p.reduceStock(quantity);
                                str += "\nAfter: " + p.toString();
                                System.out.println("--------------------product choice 3: " + p.toString());
                                return str;
                            } else {
                                return "FAIL (not enough stock)";
                            }
                        } else {
                            return "FAIL (product not found)";
                        }
                    }
                }
                return "FAIL (store not found)";
            }
            finally {
                setBeingUpdated(false);
            }
        }
    }

    //4
    private String handleAddProductInSystem(String data) {
        synchronized (lock) {
            setBeingUpdated(true);
            try{
                String[] parts = data.split(",", 5);

                if (parts.length != 5) return "FAIL (wrong format)";

                String storeName = parts[0];
                String productName = parts[1];
                String productType = parts[2];
                int quantity;
                double price;
                try {
                    quantity = Integer.parseInt(parts[3]);
                    price = Double.parseDouble(parts[4]);
                } catch (NumberFormatException e) {
                    return "FAIL (invalid quantity or price)";
                }
                for (Store s : stores) {
                    if (s.getStoreName().equalsIgnoreCase(storeName)) {
                        Product existing = s.getProductByName(productName);
                        if (existing != null) {
                            return "FAIL (product already exists)";
                        }
                        System.out.println("Choice 4: " + s.toString("manager"));
                        Product newProduct = new Product(productName, productType, quantity, price);
                        s.addProduct(newProduct);

                        System.out.println(s.toString("manager"));
                        return newProduct.toString();
                    }
                }
                return "FAIL (store not found)";
            } finally {
                setBeingUpdated(false);
            }
        }
    }

    //5
    private String handleRemoveProductFromSystem(String data) {
        synchronized (lock) {
            setBeingUpdated(true);
            try {
                String[] parts = data.split(",", 2);
                if (parts.length != 2) return "FAIL (wrong format)";

                String storeName = parts[0].trim();
                String productName = parts[1].trim();

                for (Store s : stores) {
                    if (s.getStoreName().equalsIgnoreCase(storeName)) {
                        System.out.println("Choice 5: " + s.toString("manager"));
                        Product p = s.getProductByName(productName);
                        if (p != null) {
                            System.out.println("product to remove:" + p.toString());
                            s.removeProductByName(productName);
                            System.out.println("remove : " + s.toString("manager"));
                            return "OK";
                        } else {
                            return "FAIL (product not found)";
                        }
                    }
                }
                return "FAIL (store not found)";
            }
            finally {
                setBeingUpdated(false);
            }
        }
    }

    //6
    private Map<String, Integer> handleViewSalesByProductForStore(String data) {
        synchronized (lock) {
            Map<String, Integer> result = new HashMap<>();
            System.out.println("Received request to view sales by product for store: " + data);
            for (Store store : stores) {
                if (store.getStoreName().equalsIgnoreCase(data.trim())) {
                    System.out.println("Accessing products for store: " + store.getStoreName());
                    for (Product product : store.getProducts()) {
                        String productName = product.getProductName();
                        int soldAmount = product.getSoldAmount();
                        result.put(productName, soldAmount);
                        System.out.println("Product: " + productName + " -> SoldAmount: " + soldAmount);
                    }
                }
            }
            System.out.println("Final product sales for store '" + data + "': " + result);
            return result;
        }
    }

    //7
    private Map<String, Integer> handleViewTotalSalesByCategory(String data) {
        synchronized (lock) {
            String[] parts = data.split(",", 2);
            System.out.println("Received data: " + data);
            System.out.println("Split data into parts: " + Arrays.toString(parts));

            if (parts.length != 2) {
                return Collections.emptyMap();
            }

            try {
                int subChoice;
                try {
                    subChoice = Integer.parseInt(parts[0].trim());
                    System.out.println("Parsed subChoice: " + subChoice);
                } catch (NumberFormatException e) {
                    System.out.println("Error parsing subChoice: " + parts[0].trim());
                    return Collections.emptyMap();
                }
                String category = parts[0].trim();
                String specific = parts[1].trim();

                System.out.println("Parsed category: " + category);
                System.out.println("Parsed specific: " + specific);

                Map<String, Integer> result = new HashMap<>();

                if (subChoice == 1) {// food Category
                    for (Store store : stores) {
                        int sum = 0;
                        System.out.println("Checking store: " + store.getStoreName());
                        if (store.getFoodCategory().equalsIgnoreCase(specific)) {
                            System.out.println("Store " + store.getStoreName() + " matches category: " + specific);
                            for (Product product : store.getProducts()) {
                                sum += product.getSoldAmount();
                                System.out.println("Product: " + product.getProductName() + ", SoldAmount: " + product.getSoldAmount());
                            }
                            result.put(store.getStoreName(), sum);
                            System.out.println("Added to result: " + store.getStoreName() + " -> " + sum);
                        }
                    }
                } else if (subChoice == 2) { //product
                    for (Store store : stores) {
                        int sum = 0;
                        int check = 0;
                        for (Product product : store.getProducts()) {
                            if (product.getProductType().equalsIgnoreCase(specific)) {
                                sum += product.getSoldAmount();
                                check++;
                                System.out.println("Product: " + product.getProductName() + ", SoldAmount: " + product.getSoldAmount() + " matches productType: " + specific);
                            }
                        }
                        if (check > 0) {
                            result.put(store.getStoreName(), sum);
                            System.out.println("Added to result: " + store.getStoreName() + " -> " + sum);
                        }
                    }
                }
                return result;
            }  catch (NumberFormatException e) {
                return Collections.emptyMap();
            }
        }
    }

    //8
    private Map<String, String> handleViewPriceCategory(String data) {
        synchronized (lock) {
            Map<String, String> resultΗ = new HashMap<>();
            String[] parts = data.split(",", 2);

            System.out.println("Initial data: " + data);

            if (parts.length != 2) {
                return Collections.emptyMap();
            }

            String StoreName = parts[0].trim();
            String specific = parts[1].trim();

            System.out.println("StoreName: " + StoreName);
            System.out.println("Specific store: " + specific);

            if (StoreName.equalsIgnoreCase("ALL")) {
                for (Store store : stores) {
                    String priceCategory = store.calculatePriceCategory();
                    resultΗ.put(store.getStoreName(), priceCategory);
                }
            } else {
                for (Store store : stores) {
                    if (store.getStoreName().equals(StoreName)) {
                        String priceCategory = store.calculatePriceCategory();
                        resultΗ.put(store.getStoreName(), priceCategory);
                        break;
                    }
                }
            }
            return resultΗ;
        }
    }
}