import java.io.*;
import java.net.*;
import java.util.*;

public class Client extends Thread {
    private final Scanner scanner = new Scanner(System.in);
    private int masterPort;
    private String masterIP;
    private ObjectOutputStream out=null;
    private ObjectInputStream in=null;
    private Socket requestSocket =null;

    public Client(String masterIP, int masterPort) {
        this.masterIP = masterIP;
        this.masterPort = masterPort;
    }

    public void run() {
        System.out.println("Client started, connecting to master server...");

        try {
            requestSocket = new Socket(masterIP, masterPort);
            System.out.println("Connected to Master!");
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
            showClientMenu();

        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (requestSocket != null) requestSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    private void showClientMenu() {
        int choice;
        while (true) {
            System.out.println("\n=== Client Menu ===");
            System.out.println("1. Show stores within 5 km radius");
            System.out.println("2. Filter stores by preferences");
            System.out.println("3. Buy product");
            System.out.println("4. Rate a store (1-5 stars)");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            int num=0;

            try {
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                if (choice < 1 || choice > 5) {
                    System.out.println("Please enter a number between 1-5");
                    continue;
                }

                switch (choice) {
                    case 1:
                        handleStoreRequest(choice);
                        num=1;
                        break;
                    case 2:
                        handleFilterRequest(choice);
                        num=2;
                        break;
                    case 3:
                        handlePurchaseProduct(choice);
                        num=3;
                        break;
                    case 4:
                        num=4;
                        handleRatingRequest(choice);
                        break;
                    case 5:
                        num=5;
                        System.out.println("Exiting client...");
                        out.writeObject(choice);
                        out.writeObject("shutdown");
                        out.flush();
                    default:
                        break;
                }

                if(num==5){
                    System.exit(0);
                }
            } catch (IOException e) {
                System.err.println("Error communicating with server: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Input error: " + e.getMessage());
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private void handleStoreRequest(int number) throws IOException, ClassNotFoundException {
        String data;
        try {
            System.out.println("Sending choice: " + number);
            out.writeObject(number);
            System.out.println("Sent command: " + number);
            out.writeObject("client");

            // Validate latitude input
            double latitude;
            while (true) {
                System.out.print("Enter latitude (-90 to 90): ");
                try {
                    latitude = scanner.nextDouble();
                    if (latitude >= -90 && latitude <= 90) break;
                    System.out.println("Latitude must be between -90 and 90");
                } catch (InputMismatchException e) {
                    System.out.println("Please enter a valid number");
                    scanner.next();
                }
            }

            // Validate longitude input
            double longitude;
            while (true) {
                System.out.print("Enter longitude (-180 to 180): ");
                try {
                    longitude = scanner.nextDouble();
                    if (longitude >= -180 && longitude <= 180) break;
                    System.out.println("Longitude must be between -180 and 180");
                } catch (InputMismatchException e) {
                    System.out.println("Please enter a valid number");
                    scanner.next();
                }
            }
            scanner.nextLine();

            data = latitude + "," + longitude;
            out.writeObject(data);
            out.flush();

            Object response = in.readObject();
            System.out.println("\nServer Response:\n" + response);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Communication error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Invalid input: " + e.getMessage());
        }
    }

    private void handleFilterRequest(int number) throws IOException, ClassNotFoundException {
        try {
            System.out.println("Sending choice: " + number);
            out.writeObject(number);
            System.out.println("Sent command: " + number);
            out.writeObject("client");

            Map<String, Object> filterCriteria = new HashMap<>();

            // Category filter with y/n validation
            String categoryChoice;
            while (true) {
                System.out.print("Filter by category? (y/n): ");
                categoryChoice = scanner.nextLine().trim().toLowerCase();
                if (categoryChoice.equals("y") || categoryChoice.equals("n")) {
                    break;
                }
                System.out.println("Please enter 'y' or 'n'");
            }

            if (categoryChoice.equals("y")) {
                System.out.print("Enter food category (e.g., sushi, burgers): ");
                String category = scanner.nextLine().trim();
                if (!category.isEmpty()) {
                    filterCriteria.put("category", category);
                } else {
                    System.out.println("Category cannot be empty");
                }
            }

            // Star rating with y/n validation
            String starsChoice;
            while (true) {
                System.out.print("Filter by star rating? (y/n): ");
                starsChoice = scanner.nextLine().trim().toLowerCase();
                if (starsChoice.equals("y") || starsChoice.equals("n")) {
                    break;
                }
                System.out.println("Please enter 'y' or 'n'");
            }

            if (starsChoice.equals("y")) {
                double minStars;
                while (true) {
                    System.out.print("Enter star rating (1.0-5.0): ");
                    try {
                        minStars = scanner.nextDouble();
                        if (minStars >= 1.0 && minStars <= 5.0) break;
                        System.out.println("Rating must be between 1.0 and 5.0");
                    } catch (InputMismatchException e) {
                        System.out.println("Please enter a valid number");
                        scanner.next();
                    }
                }
                scanner.nextLine();
                filterCriteria.put("Stars", minStars);
            }

            // Price category with y/n validation
            String priceChoice;
            while (true) {
                System.out.print("Filter by price category? (y/n): ");
                priceChoice = scanner.nextLine().trim().toLowerCase();
                if (priceChoice.equals("y") || priceChoice.equals("n")) {
                    break;
                }
                System.out.println("Please enter 'y' or 'n'");
            }

            if (priceChoice.equals("y")) {
                String priceCategory;
                while (true) {
                    System.out.print("Enter price category ($, $$, $$$): ");
                    priceCategory = scanner.nextLine().trim();
                    if (priceCategory.matches("^\\$+$") && priceCategory.length() <= 3) break;
                    System.out.println("Invalid price category. Must be $, $$, or $$$");
                }
                filterCriteria.put("priceLevel", priceCategory);
            }

            if (filterCriteria.isEmpty()) {
                System.out.println("No filters selected. Showing all available stores.");
            }

            out.writeObject(filterCriteria);
            out.flush();

            Object response = in.readObject();
            if (response instanceof List<?>) {
                List<?> storeList = (List<?>) response;
                System.out.println("\nFiltered Stores:");
                for (Object obj : storeList) {
                    if (obj instanceof Store) {
                        System.out.println((Store) obj);
                    } else {
                        System.out.println(obj);
                    }
                }
            } else {
                System.out.println("\nServer Response:\n" + response);
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Communication error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Invalid input: " + e.getMessage());
            scanner.nextLine();
        }
    }

    private void handleRatingRequest(int number) throws IOException, ClassNotFoundException {
        try {
            System.out.println("Sending choice: " + number);
            out.writeObject(number);
            System.out.println("Sent command: " + number);
            out.writeObject("client");

            System.out.print("Enter the name of the store you want to rate: ");
            String storeName = scanner.nextLine().trim();

            System.out.print("Enter your rating (1 to 5): ");
            int rating = scanner.nextInt();
            scanner.nextLine(); // consume newline

            if (rating < 1 || rating > 5) {
                System.out.println("Rating must be between 1 and 5.");
                return;
            }

            String data=storeName + "," + rating;
            out.writeObject(data);
            out.flush();

            Object response = in.readObject();
            System.out.println("\nServer Response:\n" + response);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Communication error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Invalid input: " + e.getMessage());
            scanner.nextLine();
        }
    }

    private void handlePurchaseProduct(int number) throws IOException, ClassNotFoundException {
        try {
            System.out.println("Sending choice: " + number);
            out.writeObject(number);
            System.out.println("Sent command: " + number);
            out.writeObject("client");

            System.out.print("Enter store name: ");
            String storeName = scanner.nextLine().trim();

            System.out.println("Enter products and quantities in the format: name:quantity,name:quantity (e.g., tylichto:1,pita:2,patates:1): ");
            String productList = scanner.nextLine().trim();

            String purchaseData = storeName + "|" + productList;
            out.writeObject(purchaseData);
            out.flush();

            Object response = in.readObject();
            System.out.println("Server Response: " + response);

        } catch (NumberFormatException e) {
            System.err.println("Invalid quantity format. Must be an integer.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Communication error: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        String ip = "127.0.0.1"; // default IP
        int port = 5000;         // default port

        if (args.length > 0) {
            ip = args[0];
        }

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port. Using default port 5000.");
            }
        }

        new Client(ip, port).start();
    }
}