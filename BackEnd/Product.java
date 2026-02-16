import java.util.*;
import java.io.Serializable;

public class Product implements Serializable {
    private String productName;
    private String productType;
    private int availableAmount;
    private int firstValue;
    private double price;

    public Product(String productName, String productType, int availableAmount, double price) {
        this.productName = productName;
        this.productType = productType;
        this.availableAmount = availableAmount;
        this.firstValue=availableAmount;
        this.price = price;
    }

    public String getProductName() { 
        return productName; 
    }
    public String getProductType() { 
        return productType; 
    }
    public int getAvailableAmount() { 
        return availableAmount; 
    }
    public int getFirstValue() {return firstValue;}
    public double getPrice() {
        return price; 
    }

    public void addStock(int amount) {
        if (amount > 0) {
            availableAmount += amount;
            this.firstValue += amount;
        } else {
            System.out.println("Invalid amount to add.");
        }
    }

    public int getSoldAmount() {
        if(isAvailable()){
            return firstValue - availableAmount ;
        }
        return firstValue;
    }

    public double getTotalSales() {
        return getSoldAmount() * price;
    }

    public double getTotalCost(int amount) {
        if(isAvailable() && hasProduct(amount) ) {
            return amount * price;
        }
        return 0.0;
    }

    public  boolean hasProduct(int amount) {
        return availableAmount-amount > 0;
    }
    public boolean isAvailable() {
        return availableAmount > 0;
    }

    public void updatePrice(double newPrice) {
        if (newPrice > 0) {
            this.price = newPrice;
        } else {
            System.out.println("Invalid price.");
        }
    }

    public void reduceStock(int amount) {
        if (amount <= availableAmount) {
            availableAmount -= amount;
        } else {
            System.out.println("Not enough stock for " + productName);
            availableAmount=getAvailableAmount();
        }
    }

    public static Product getProductByName(String productName, List<Product> products) {
        for (Product product : products) {
            if (product.getProductName().equalsIgnoreCase(productName)) {
                return product;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "productName: "+productName + " (productType:" + productType + ") -price: " + price + " available: " + availableAmount + "\n";
    }
}
