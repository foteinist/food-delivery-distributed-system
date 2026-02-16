import java.io.*;
import java.net.*;
import java.util.*;


public class Manager extends Thread{

    private Scanner scanner = new Scanner(System.in);
    private ObjectOutputStream out=null;
    private ObjectInputStream in=null;
    private Socket requestSocket =null;
    private int masterPort;
    private String masterIP;

    public Manager(String masterIP, int masterPort) {
        this.masterIP = masterIP;
        this.masterPort = masterPort;
    }
    
    public void run() {

        System.out.println("Manager is ready and listening on port " + masterPort);
        try {
            // Σύνδεση με server στο localhost, θύρα 5000
            //192.168.56.1
            //requestSocket = new Socket("192.168.56.1", masterPort);
            requestSocket = new Socket(masterIP, masterPort);
            System.out.println("Connected to Master!");
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
            showManagerMenu();
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

    private void showManagerMenu() {
        int choice;
        while(true) {
            System.out.println("===== Manager Menu =====");
            System.out.println("1. Add Store");//
           // System.out.println("2. Update Store");
            System.out.println("2. Add Available Product");//
            System.out.println("3. Remove Available Product");//
            System.out.println("4. Add Product");
            System.out.println("5. Remove Product");
            System.out.println("6. View Total Sales per Store");
            System.out.println("7. View Sales by Food Category or Product Category");
            System.out.println("8. Calculate Store Price Category");
            System.out.println("9. Exit");
            System.out.print("\nChoose an option: ");

            try {
                choice = scanner.nextInt();
                scanner.nextLine();

                if (choice < 1 || choice > 9) {
                    System.out.println("Please enter a number between 1-9");
                    continue;
                }

                handleManagerChoice(choice);

            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
                choice = 0;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                break;
            }
        }
    }

    private void handleManagerChoice(int number) throws IOException, ClassNotFoundException {
        try {
            System.out.println("Sending choice: " + number);
            out.writeObject(number);
            System.out.println("Sent command: " + number);
            out.writeObject("manager");

            String data = "";

            if (number == 1) {
                System.out.print("Enter folder name containing store JSON and logo: ");
                data = scanner.nextLine();
                out.writeObject(data);
                out.flush();

            }
            else if (number == 2) {
                System.out.print("Enter store name: ");
                String storeName = scanner.nextLine();
                System.out.print("Enter product name: ");
                String productName = scanner.nextLine();
                System.out.print("Enter quantity: ");
                int quantity = scanner.nextInt();
                scanner.nextLine();
                data = storeName + "," + productName + "," + quantity;
                out.writeObject(data);
                out.flush();

            } else if (number == 3) {
                System.out.print("Enter store name: ");
                String storeName = scanner.nextLine();
                System.out.print("Enter product name: ");
                String productName = scanner.nextLine();
                System.out.print("Enter quantity: ");
                int quantity = scanner.nextInt();
                scanner.nextLine();
                data = storeName + "," + productName + "," + quantity;
                out.writeObject(data);
                out.flush();

            } else if (number == 4) {
                System.out.print("Enter the store: ");
                String storeNames = scanner.nextLine();
                System.out.print("Enter new product name to add to the system: ");
                String newProductName = scanner.nextLine();
                System.out.print("Enter product type: ");
                String newProductType = scanner.nextLine();
                System.out.print("Enter available amount: ");
                int newAvailableAmount = Integer.parseInt(scanner.nextLine());
                System.out.print("Enter price: ");
                double newPrice = Double.parseDouble(scanner.nextLine());
                data= storeNames + ", Product " +newProductName + "," + newProductType + "," + newAvailableAmount + "," + newPrice;
                out.writeObject(data);
                out.flush();

            } else if (number == 5) {
                System.out.print("Enter the store: ");
                String storeNames = scanner.nextLine();
                System.out.print("Enter product name to remove from the system: ");
                String productName = scanner.nextLine();
                data= storeNames + ", Product " +productName;

                out.writeObject(data);
                out.flush();

            } else if (number == 6) {
                System.out.print("Enter the store: ");
                String storeName = scanner.nextLine();
                out.writeObject(storeName);
                out.flush();
            } else if(number == 7) {
                System.out.print("Do you want to search by (1) Food Category or (2) Product Category? ");
                int subChoice = scanner.nextInt();
                scanner.nextLine();
                if (subChoice == 1) {
                    System.out.print("Enter Food Category (e.g., pizzeria): ");
                    String category = scanner.nextLine();
                    out.writeObject(subChoice);
                    out.writeObject(category);
                    out.flush();

                } else if (subChoice == 2) {
                    System.out.print("Enter Product Category (e.g., salad): ");
                    String productCategory = scanner.nextLine();
                    out.writeObject(subChoice);
                    out.writeObject(productCategory);
                    out.flush();
                } else {
                    System.out.println("Invalid choice.");
                    return;
                }
            }else if (number == 8) {
                System.out.println("Do you want to view the price category for a single store, multiple stores, or all stores?");
                System.out.println("1. Single store");
                System.out.println("2. All stores");
                System.out.print("Enter choice (1/2): ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                if (choice == 1) {
                    System.out.print("Enter the store: ");
                    String inputStores = scanner.nextLine();
                    out.writeObject(choice);
                    out.writeObject(inputStores);
                    out.flush();

                }else if(choice == 2){
                    out.writeObject(choice);
                    out.writeObject("ALL");
                    out.flush();
                }
            }
            else if (number == 9) {
                System.out.println("Exiting Manager...");
                out.writeObject(number);
                out.writeObject("shutdown");
                out.flush();
            }

            if( number!=9){
                Object response = in.readObject();
                if(number==6 ||number==7 || number==8){
                    System.out.println("Master response:\n " + response);
                }
                else {
                    System.out.println("Master response: " + response);
                }
            }

            if(number == 9) {
                System.exit(0);
            }

        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                System.err.println("Error: Could not connect to the Master. Please check the server address and try again.");
            } else if (e instanceof SocketTimeoutException) {
                System.err.println("Error: Connection timed out. Please check the Master or your network connection.");
            } else {
                System.err.println("Communication error: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error: The expected class could not be found. Please check the server implementation.");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
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

        new Manager(ip, port).start();
    }
}