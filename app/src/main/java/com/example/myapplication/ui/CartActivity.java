package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import java.util.ArrayList;


public class CartActivity extends AppCompatActivity {
    ListView cartListView;
    TextView totalItemsTextView, totalCostTextView,totalTextView,storeNameTextView;
    Button sendOrderButton;
    String storeName, serverIp;
    int serverPort;
    ArrayList<MyObject> cartItems;
    int totalItems;
    double totalCost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        Intent intent = getIntent();
        cartItems = (ArrayList<MyObject>) intent.getSerializableExtra("CART_ITEMS");
        serverIp = intent.getStringExtra("SERVER_IP");
        serverPort = intent.getIntExtra("SERVER_PORT", -1);
        storeName = intent.getStringExtra("STORE_NAME");
        totalItems = getIntent().getIntExtra("CART_ITEMS_COUNT", 0);
        totalCost = getIntent().getDoubleExtra("CART_TOTAL", 0.0);


        cartListView = findViewById(R.id.cartListView);
        totalTextView = findViewById(R.id.totalTextView);
        sendOrderButton = findViewById(R.id.sendOrderButton);
        totalItemsTextView = findViewById(R.id.totalItemsTextView);
        totalCostTextView = findViewById(R.id.totalCostTextView);
        storeNameTextView = findViewById(R.id.storeNameTextView);

        if (storeName != null) {
            storeNameTextView.setText("Store: " + storeName);
        }

        totalItemsTextView.setText("Items: " + totalItems);
        totalCostTextView.setText("Total Cost: €" + String.format("%.2f", totalCost));

        ProductAdapter adapter = new ProductAdapter(this, cartItems, null, false, null);
        cartListView.setAdapter(adapter);

        sendOrderButton.setOnClickListener(v -> {
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == 2) {
                        String serverResponse = (String) msg.obj;
                        Toast.makeText(CartActivity.this, "Server Response: " + serverResponse, Toast.LENGTH_LONG).show();

                        if (serverResponse.toLowerCase().contains("success")) {
                            new androidx.appcompat.app.AlertDialog.Builder(CartActivity.this)
                                    .setTitle("Success")
                                    .setMessage("Your order has been placed successfully!")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        Intent intent = new Intent(CartActivity.this, RatingActivity.class);
                                        intent.putExtra("SERVER_IP", serverIp);
                                        intent.putExtra("SERVER_PORT", serverPort);
                                        intent.putExtra("STORE_NAME", storeName);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                        }
                    } else if (msg.what == -1) {
                        // Show error message in case of failure
                        String error = (String) msg.obj;
                        Toast.makeText(CartActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            };
            new MyThread(handler, cartItems, serverIp, serverPort, 2,storeName).start();
        });
    }

    private ArrayList<String> convertCartToStrings(ArrayList<MyObject> cart) {
        ArrayList<String> list = new ArrayList<>();
        for (MyObject item : cart) {
            list.add(item.getName() + " x" + item.getQuantity() + " - €" +
                    String.format("%.2f", item.getQuantity() * item.getPrice()));
        }
        return list;
    }
}
