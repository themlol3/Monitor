package com.example.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {

    private TextView logTextView;
    private TextView clear;
    private StringBuilder logData = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        clear = findViewById(R.id.clear);

        ImageView grIV = findViewById(R.id.grIV);
        ImageView wsIV = findViewById(R.id.wsIV);

        grIV.setOnClickListener(v -> showNetworkOptionsDialog());
        wsIV.setOnClickListener(v -> showWebSocketOptionsDialog());
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logTextView.setText("");
                logData = new StringBuilder();
            }
        });

        getSupportActionBar().hide();

        LocalBroadcastManager.getInstance(this).registerReceiver(logReceiver, new IntentFilter(NetworkMonitorService.ACTION_LOG_UPDATE));
    }

    private void showNetworkOptionsDialog() {
        final String[] options = {"https://jsonplaceholder.typicode.com/posts", "https://httpbin.org/get", "Custom URL"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose where to send an HTTP Get Request");

        builder.setItems(options, (dialog, which) -> {
            if (which == 2) {
                showCustomUrlDialog("network");
            } else {
                String url = options[which];
                startNetworkService(url, "network");
            }
        });

        builder.show();
    }

    private void showWebSocketOptionsDialog() {
        final String[] options = {"wss://echo.websocket.org", "wss://demos.kaazing.com/echo", "Custom URL"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose where to send a WebSocket");

        builder.setItems(options, (dialog, which) -> {
            if (which == 2) {
                showCustomUrlDialog("websocket");
            } else {
                String url = options[which];
                startNetworkService(url, "websocket");
            }
        });

        builder.show();
    }

    private void showCustomUrlDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter " + type + " URL");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String url = input.getText().toString();
            if (!url.isEmpty()) {
                startNetworkService(url, type);
            } else {
                Toast.makeText(MainActivity.this, "Please type a URL", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void startNetworkService(String url, String type) {
        Intent intent = new Intent(this, NetworkMonitorService.class);
        intent.putExtra("url", url);
        intent.putExtra("type", type);
        startService(intent);
    }

    private BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("log")) {
                String logMessage = intent.getStringExtra("log");
                logData.append(logMessage).append("\n");
                logTextView.setText(logData.toString());
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver);
    }
}
