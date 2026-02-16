package com.example.myapplication.ui;

import android.content.Intent;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    EditText ipEditText, portEditText;
    Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipEditText = findViewById(R.id.ipEditText);
        portEditText = findViewById(R.id.portEditText);
        connectButton = findViewById(R.id.connectButton);

        connectButton.setOnClickListener(v -> {
            String ip = ipEditText.getText().toString().trim();
            String portStr = portEditText.getText().toString().trim();

            if(ip.isEmpty() || portStr.isEmpty()) {
                Toast.makeText(this, "Please enter IP and port", Toast.LENGTH_SHORT).show();
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
                return;
            }

            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {

                    if (msg.what == 1) {
                        Toast.makeText(MainActivity.this, "Connected successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                        intent.putExtra("SERVER_IP", ip);
                        intent.putExtra("SERVER_PORT", port);
                        intent.putExtra("STORE_NAME", "Pizza Paradise");
                        startActivity(intent);
                    } else {
                        String errorMsg = (String) msg.obj;
                        Toast.makeText(MainActivity.this, "Connection failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            };
            new MyThread(handler, new ArrayList<>(), ip, port,0).start();
        });
    }
}
