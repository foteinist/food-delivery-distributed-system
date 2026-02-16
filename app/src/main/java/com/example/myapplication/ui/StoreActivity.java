package com.example.myapplication.ui;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import java.util.*;

public class StoreActivity extends AppCompatActivity {

    private static final int REQUEST_ORDER = 1;
    private static final int REQUEST_CART = 2;
    private static final int REQUEST_RATING = 3;

    // Starts the buying process by launching OrderActivity first
    public void buy() {
        Intent orderIntent = new Intent(this, OrderActivity.class);
        startActivityForResult(orderIntent, REQUEST_ORDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ORDER:
                    // After OrderActivity finishes, start CartActivity
                    Intent cartIntent = new Intent(this, CartActivity.class);
                    startActivityForResult(cartIntent, REQUEST_CART);
                    break;

                case REQUEST_CART:
                    // After CartActivity finishes, start RatingActivity
                    Intent ratingIntent = new Intent(this, RatingActivity.class);
                    startActivityForResult(ratingIntent, REQUEST_RATING);
                    break;

                case REQUEST_RATING:
                    // After RatingActivity finishes, buying process is complete
                    Toast.makeText(this, "Purchase completed successfully!", Toast.LENGTH_LONG).show();
                    break;
            }
        } else {
            // If the user cancels at any step, you can handle it here (e.g. show a message)
            Toast.makeText(this, "Purchase process was cancelled.", Toast.LENGTH_SHORT).show();
        }
    }
}
