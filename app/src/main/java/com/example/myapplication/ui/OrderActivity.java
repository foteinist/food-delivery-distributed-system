package com.example.myapplication.ui;
import android.os.*;
import android.widget.*;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.example.myapplication.R;
import java.util.*;
public class OrderActivity extends AppCompatActivity {
    ListView listView;
    ArrayList<MyObject> items = new ArrayList<>();
    ArrayList<MyObject> cart = new ArrayList<>();
    Handler handler;
    String serverIp;
    int serverPort;
    int totalItems;
    double totalCost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        listView = findViewById(R.id.listView);
        Log.d("OrderActivity", "listView found");

        addDefaultProducts();
        ProductAdapter adapter = new ProductAdapter(this, items, cart, true, updatedCart -> {
            runOnUiThread(() -> {
                TextView cartItemCount = findViewById(R.id.cartItemCount);
                TextView cartTotal = findViewById(R.id.cartTotal);

                int totalItems = 0;
                for (MyObject item : updatedCart) {
                    totalItems += item.getQuantity();
                }
                cartItemCount.setText(totalItems + " items");

                double total = 0.0;
                for (MyObject item : updatedCart) {
                    total += item.getPrice() * item.getQuantity();
                }
                cartTotal.setText("€" + String.format("%.2f", total));
            });
        });
        listView.setAdapter(adapter);

        Intent intent = getIntent();
        serverIp = intent.getStringExtra("SERVER_IP");
        serverPort = intent.getIntExtra("SERVER_PORT", -1);

        TextView storeNameTextView = findViewById(R.id.storeNameTextView);
        String storeName = intent.getStringExtra("STORE_NAME");

        if (storeName != null) {
            storeNameTextView.setText(storeName);
        }

        Log.d("OrderActivity", "Got IP: " + serverIp + " and Port: " + serverPort);

        handler = new Handler(Looper.getMainLooper(), message -> {
            if (message.what == 1) {
                Log.d("OrderActivity", "Received message to update UI");
                ProductAdapter adapter1 = new ProductAdapter(OrderActivity.this, items, cart, true, updatedCart -> {
                    runOnUiThread(() -> {
                        TextView cartItemCount = findViewById(R.id.cartItemCount);
                        TextView cartTotal = findViewById(R.id.cartTotal);

                        int totalItems = 0;
                        for (MyObject item : updatedCart) {
                            totalItems += item.getQuantity();
                        }
                        cartItemCount.setText(totalItems + " items");

                        double total = 0.0;
                        for (MyObject item : updatedCart) {
                            total += item.getPrice() * item.getQuantity();
                        }
                        cartTotal.setText("€" + String.format("%.2f", total));
                    });
                });
                listView.setAdapter(adapter1);
            }
            return false;
        });


        TextView cartItemCount = findViewById(R.id.cartItemCount);

        TextView cartTotal = findViewById(R.id.cartTotal);

        LinearLayout cartSummaryLayout = findViewById(R.id.cartSummaryLayout);

        cartSummaryLayout.setOnClickListener(v -> {
            totalItems = 0;
            totalCost = 0.0;
            for (MyObject item : cart) {
                totalItems += item.getQuantity();
                totalCost += item.getQuantity() * item.getPrice();
            }
            Intent GOintent = new Intent(OrderActivity.this, CartActivity.class);
            GOintent.putExtra("CART_ITEMS", cart);
            GOintent.putExtra("SERVER_IP", serverIp);
            GOintent.putExtra("SERVER_PORT", serverPort);
            GOintent.putExtra("STORE_NAME", storeName);
            GOintent.putExtra("CART_TOTAL", totalCost);
            GOintent.putExtra("CART_ITEMS_COUNT", totalItems);
            startActivity(GOintent);
        });

        Runnable updateCartInfo = () -> {
            Log.d("OrderActivity", "Updating cart info...");
            int itemCount = 0;
            double total = 0.0;
            for (MyObject item : cart) {
                itemCount += item.getQuantity();
                total += item.getPrice();
            }
            cartItemCount.setText(itemCount + " items");
            cartTotal.setText("€" + String.format("%.2f", total));

            Log.d("OrderActivity", "Cart now has " + itemCount + " items, total = " + total);
        };

        listView.setOnItemClickListener((parent, view, position, id) -> {
            MyObject selected = items.get(position);
            boolean found = false;
            for (MyObject item : cart) {
                if (item.getName().equals(selected.getName())) {
                    item.setQuantity(item.getQuantity() + 1);
                    Log.d("OrderActivity", "Updated quantity for " + item.getName() + " to " + item.getQuantity());
                    found = true;
                    break;
                }
            }
            if (!found) {
                MyObject newItem = new MyObject(selected.getName(), selected.getPrice(), selected.getImageUrl());
                newItem.setQuantity(1);
                cart.add(newItem);
                Log.d("OrderActivity", "Added new item " + newItem.getName());
            }

            Toast.makeText(OrderActivity.this, selected.getName() + " added to cart ", Toast.LENGTH_SHORT).show();
            updateCartInfo.run();
        });

    }
    private void addDefaultProducts() {
        items.clear();
        items.add(new MyObject("pepperoni", 1.50, "https://cdn.pixabay.com/photo/2016/03/05/19/02/pizza-1238246_1280.jpg"));
        items.add(new MyObject("veggie", 10, "https://th.bing.com/th/id/OIP.fnSnpZO4_MZAaJEc6JVJSAHaLH?cb=iwc2&rs=1&pid=ImgDetMain.jpg"));
        items.add(new MyObject("margherita", 7.5, "https://cdn.pixabay.com/photo/2017/12/09/08/18/pizza-3007395_1280.jpg"));
        items.add(new MyObject("greek salad", 6.00, "https://www.modernhoney.com/wp-content/uploads/2023/03/Greek-Salad-2-scaled.jpg"));
        items.add(new MyObject("classic burger", 5.50, "https://cdn.pixabay.com/photo/2014/10/23/18/05/burger-500054_1280.jpg"));

        Log.d("OrderActivity", "Added " + items.size() + " default products");
    }
}
