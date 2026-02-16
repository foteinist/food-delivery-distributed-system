package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.util.Log;  // στην αρχή

public class RatingActivity extends AppCompatActivity {

    private ImageView[] stars;
    private int currentRating = 0;
    private TextView storeNameText;
    String serverIp;
    int serverPort;
    String storeName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        Intent intent = getIntent();
        serverIp = intent.getStringExtra("SERVER_IP");
        serverPort = intent.getIntExtra("SERVER_PORT", -1);
        storeName = intent.getStringExtra("STORE_NAME");

        storeNameText = findViewById(R.id.storeNameText);

        if (storeName != null && !storeName.isEmpty()) {
            storeNameText.setText(storeName);
        } else {
            storeNameText.setText("Shop");
        }

        ImageView storeLogo = findViewById(R.id.storeLogo);
        String logoUrl = intent.getStringExtra("STORE_LOGO_URL");

        storeLogo.setImageResource(R.drawable.store_logo); // default

        stars = new ImageView[5];
        stars[0] = findViewById(R.id.star1);
        stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3);
        stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5);

        for (int i = 0; i < stars.length; i++) {
            final int index = i;
            stars[i].setOnClickListener(v -> {
                setRating(index + 1);
            });
        }

        Log.d("RatingDebug", "SERVER_IP: " + serverIp + ", SERVER_PORT: " + serverPort + ", STORE_NAME: " + storeName);

        Button backToSearchButton = findViewById(R.id.backToSearchButton);
        backToSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("RatingDebug", "Back button clicked. Sending rating: " + currentRating);

                Handler handler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == 3) {
                            Log.d("RatingDebug", "Server responded: " + msg.obj);

                            String serverResponse = (String) msg.obj;
                            Toast.makeText(RatingActivity.this, "Server Response: " + serverResponse, Toast.LENGTH_LONG).show();

                            if (serverResponse.toLowerCase().contains("successfully")) {
                                new androidx.appcompat.app.AlertDialog.Builder(RatingActivity.this)
                                        .setTitle("Thank you!")
                                        .setMessage("Your rating has been submitted successfully!")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            Intent intent = new Intent(RatingActivity.this, OrderActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                                            startActivity(intent);
                                            finish();
                                        })
                                        .setCancelable(false)
                                        .show();
                            }
                        } else if (msg.what == -1) {
                            Log.e("RatingDebug", "Error from server: " + msg.obj);

                            String error = (String) msg.obj;
                            Toast.makeText(RatingActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                };
                new MyThread(handler, currentRating, serverIp, serverPort, 3, storeName).start();
            }
        });

    }

    private void setRating(int rating) {
        currentRating = rating;
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.star_yellow);
            } else {
                stars[i].setImageResource(R.drawable.star_grey);
            }
        }
    }
}
