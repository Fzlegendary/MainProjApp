package com.example.mainproj.messaging;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkMonitor {

    private static NetworkMonitor instance;
    private final ConnectivityManager connectivityManager;

    private NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isConnected() {
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public void registerNetworkCallback(ConnectivityManager.NetworkCallback callback) {
        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(callback);
        }
    }

    public void unregisterNetworkCallback(ConnectivityManager.NetworkCallback callback) {
        if (connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(callback);
            } catch (Exception ignored) {
            }
        }
    }
}