import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public class StoreLoader {
    public Store readStoreFromFile(String jsonFilePath) throws IOException {
        Gson gson = new Gson();

        try (Reader reader = new FileReader("BigStore/" + jsonFilePath + ".json")) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            String storeName = jsonObject.get("storeName").getAsString();
            double latitude = jsonObject.get("latitude").getAsDouble();
            double longitude = jsonObject.get("longitude").getAsDouble();
            String foodCategory = jsonObject.get("foodCategory").getAsString();
            int stars = jsonObject.get("stars").getAsInt();
            int noOfVotes = jsonObject.get("noOfVotes").getAsInt();
            String storeLogo = jsonObject.get("storeLogo").getAsString();

            JsonArray productsArray = jsonObject.getAsJsonArray("products");
            List<Product> products = new ArrayList<>();

            for (JsonElement productElement : productsArray) {
                JsonObject productObject = productElement.getAsJsonObject();
                String productName = productObject.get("productName").getAsString();
                String productType = productObject.get("productType").getAsString();
                int availableAmount = productObject.get("availableAmount").getAsInt();
                double price = productObject.get("price").getAsDouble();
                products.add(new Product(productName, productType, availableAmount, price));
            }

            return new Store(storeName, latitude, longitude, foodCategory, stars, noOfVotes, storeLogo, products);

        } catch (IOException e) {
            System.err.println("Error reading the JSON file: " + e.getMessage());
            throw e;
        }
    }


    public List<Store> loadStores(String jsonFilePath) throws IOException {
        Gson gson = new Gson();
        List<Store> stores = new ArrayList<>();

        try (Reader reader = new FileReader(jsonFilePath)) {
            JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();

                String name = obj.get("storeName").getAsString();
                double latitude = obj.get("latitude").getAsDouble();
                double longitude = obj.get("longitude").getAsDouble();
                String category = obj.get("foodCategory").getAsString();
                int stars = obj.get("stars").getAsInt();
                int votes = obj.get("noOfVotes").getAsInt();
                String logo = obj.get("storeLogo").getAsString();

                List<Product> productList = new ArrayList<>();

                JsonArray productsArray = obj.getAsJsonArray("products");
                for (JsonElement pElement : productsArray) {
                    JsonObject pObj = pElement.getAsJsonObject();
                    String productName = pObj.get("name").getAsString();
                    String productType = pObj.get("type").getAsString();
                    int stock = pObj.get("stock").getAsInt();
                    double price = pObj.get("price").getAsDouble();

                    Product product = new Product(productName, productType, stock, price);
                    productList.add(product);
                }
                Store store = new Store(name, latitude, longitude, category, stars, votes, logo, productList);
                stores.add(store);
            }
        } catch (IOException e) {
            System.err.println("Error reading : " + e.getMessage());
            throw e;
        }
        return stores;
    }
}