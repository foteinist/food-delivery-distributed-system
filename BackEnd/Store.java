import java.util.*;
import java.io.Serializable;
import static java.lang.Math.*;

public class Store implements Serializable {
    private static final long serialVersionUID = 1L;
    private String storeName;
    private double latitude;
    private double longitude;
    private String foodCategory;
    private int stars;
    private int noOfVotes;
    private String storeLogo;
    private List<Product> products;
    private String priceCategory;


    public Store(String storeName, double latitude, double longitude, String foodCategory, int stars, int noOfVotes, String storeLogo, List<Product> products) {
        this.storeName = storeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategory = foodCategory;
        this.stars = stars;
        this.noOfVotes = noOfVotes;
        this.storeLogo = storeLogo;
        this.products = products;
        this.priceCategory = calculatePriceCategory();
    }

    public String getStoreName() {
        return storeName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public int getStars() {
        return stars;
    }

    public int getNoOfVotes() {
        return noOfVotes;
    }

    public String getStoreLogo() {
        return storeLogo;
    }

    public List<Product> getProducts() {
        return products;
    }

    public String getPriceCategory() {
        return priceCategory;
    }

    public void rateStore(int newRating) {
        this.stars = (Integer) (this.stars * this.noOfVotes + newRating) / (this.noOfVotes + 1);
        this.noOfVotes++;
    }

    public Product getProductByName(String name) {
        for (Product p : products) {
            if (p.getProductName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public boolean removeProductByName(String name) {
        Iterator<Product> iterator = products.iterator();
        while (iterator.hasNext()) {
            Product p = iterator.next();
            if (p.getProductName().equalsIgnoreCase(name)) {
                iterator.remove();
                this.priceCategory = calculatePriceCategory();
                return true;
            }
        }
        return false;
    }

    public void addProduct(Product product) {
        products.add(product);
        this.priceCategory = calculatePriceCategory();
    }

    public String calculatePriceCategory() {
        if (products == null || products.isEmpty()) return "empty";
        double total = 0;
        for (Product p : products) {
            total += p.getPrice();
        }
        double avg = total / products.size();
        if (avg <= 5) {
            return "$";
        } else if (avg <= 15) {
            return "$$";
        } else {
            return "$$$";
        }
    }

    public double calculateAveragePrice() {
        if (products == null || products.isEmpty()) return 0.0;

        double sum = 0;
        int count = 0;

        for (Product p : products) {
            sum += p.getPrice();
            count++;
        }

        return count > 0 ? sum / count : 0.0;
    }

    public double calculateDistanceTo(double clientLat, double clientLon) {
        final int R = 6371; // Earth radius in km

        double latDistance = toRadians(this.latitude - clientLat);
        double lonDistance = toRadians(this.longitude - clientLon);
        double a = sin(latDistance / 2) * sin(latDistance / 2)
                + cos(toRadians(clientLat)) * cos(toRadians(this.latitude))
                * sin(lonDistance / 2) * sin(lonDistance / 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));

        return R * c;
    }

    public String toString(String user) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "\n=== %s ===\n" +
                        "Category       : %s\n" +
                        "Rating         : %d/5 (%d votes)\n" +
                        "Location       : %.6f, %.6f\n" +
                        "Avg. Price     : $%.2f\n",
                storeName,
                foodCategory,
                stars,
                noOfVotes,
                latitude,
                longitude,
                calculateAveragePrice()
        ));

        if (!products.isEmpty()) {
            sb.append("Available Products:\n");

            for (Product p : products) {
                if (user.equals("client")) {
                    if (p.getAvailableAmount() > 0) {
                        sb.append(String.format(
                                "  - %-20s | $%.2f | %d in stock\n",
                                p.getProductName(),
                                p.getPrice(),
                                p.getAvailableAmount()
                        ));
                    }
                } else {
                    sb.append(String.format(
                            "  - %-20s | $%.2f | %d in stock\n",
                            p.getProductName(),
                            p.getPrice(),
                            p.getAvailableAmount()
                    ));
                }
            }
        }
        return sb.toString();
    }
}