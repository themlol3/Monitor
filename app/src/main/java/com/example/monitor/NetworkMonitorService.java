package com.example.monitor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;

public class NetworkMonitorService extends Service {

    public static final String ACTION_LOG_UPDATE = "com.example.monitor.LOG_UPDATE";
    private OkHttpClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                sendLogUpdate(message);
            }
        });
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String url = intent.getStringExtra("url");
            String type = intent.getStringExtra("type");

            if (url != null && type != null) {
                Log.d("NetworkMonitorService", "Received URL: " + url + ", Type: " + type);

                if ("network".equals(type)) {
                    performNetworkRequest(url);
                } else if ("websocket".equals(type)) {
                    performWebSocketActivity(url);
                }
            }
        }

        return START_NOT_STICKY;
    }


    private void performNetworkRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                sendLogUpdate("Get request completed: " + response.body().string());
            }
        });
    }

    private void performWebSocketActivity(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send("Hello WebSocket");
                sendLogUpdate("WebSocket opened: " + url);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                sendLogUpdate("Received message: " + text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                sendLogUpdate("WebSocket closed: " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                t.printStackTrace();
                sendLogUpdate("WebSocket failed: " + t.getMessage());
            }
        });
    }

    private void sendLogUpdate(String logMessage) {
        Intent intent = new Intent(ACTION_LOG_UPDATE);
        intent.putExtra("log", logMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
