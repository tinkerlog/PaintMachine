package com.tinkerlog.printclient.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.tinkerlog.printclient.network.ComThread;
import com.tinkerlog.printclient.network.NetworkHelper;
import com.tinkerlog.printclient.network.NetworkScanner;
import com.tinkerlog.printclient.util.ResponseHandler;

import java.util.ArrayList;
import java.util.List;

public class Printer {

    private static final String TAG = "Printer";

    public enum State {
        NONE,
        SEARCHING_PRINTER,
        CONNECTING_PRINTER,
        CONNECTED,
        DISCONNECTING_PRINTER,
        DISCONNECTED,
        ERRORED
    }

    public enum Action {
        START_SEARCH,
        FOUND_PRINTER,
        CONNECTED_TO_PRINTER,
        DROPPED_FROM_NETWORK,
        DISCONNECT_PRINTER,
        NETWORK_ERROR
    }

    public interface StatusCallback {
        public void onStatusChanged(State state, String message);
    }

    public static final String PRINTER_SSID = "PRINTBOT";
    public static final String PRINTER_SSID_QUOTED = "\"PRINTBOT\"";

    private Context context;
    private WifiManager wifiManager;
    private String ip;
    private State state;
    private WifiChangeReceiver wifiChangeReceiver;
    private NetworkScanner networkScanner;
    private ComThread comThread;
    private List<StatusCallback> callbacks = new ArrayList<>();

    public Printer(Context context) {
        this.context = context;
        wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        state = State.NONE;
    }

    public void onResume() {
    }

    public void addStatusCallback(StatusCallback callback) {
        callbacks.add(callback);
    }

    public void removeStatusCallback(StatusCallback callback) {
        callbacks.remove(callback);
    }

    public State getState() {
        return state;
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    public void connect() {
        input(Action.START_SEARCH);
    }

    public void disconnect() {
        input(Action.DISCONNECT_PRINTER);
    }

    public void send(Command command) {
        if (!isConnected()) {
            Log.w(TAG, "printer not connected!");
        }
        else if (comThread != null) {
            comThread.send(command);
        }
    }

    public void sendIfEmpty(Command command) {
        if (!isConnected()) {
            Log.w(TAG, "printer not connected!");
        }
        else if (comThread != null) {
            comThread.sendIfEmpty(command);
        }
    }

    public void requestStatus(ResponseHandler handler) {
        Command cmd = new StatusCommand(handler);
        send(cmd);
    }

    private void input(Action input) {
        State oldState = state;
        switch (state) {
            case DISCONNECTED:
            case NONE:
            case ERRORED:
                if (input == Action.START_SEARCH) {
                    if (NetworkHelper.isOnPrinterWifi(wifiManager)) {
                        state = State.CONNECTED;
                        updateStatus(state, "already on printer wifi.");
                        startComThread();
                    }
                    else {
                        state = State.SEARCHING_PRINTER;
                        startNetworkScanner();
                        registerWifiChange();
                        updateStatus(state, "searching for printer ...");
                    }
                }
                break;
            case SEARCHING_PRINTER:
                switch (input) {
                    case FOUND_PRINTER:
                        updateStatus(state, "connecting to printer ...");
                        NetworkHelper.connectToPrinter(wifiManager, new Runnable() {
                            @Override
                            public void run() {
                                wifiChangeReceiver.enabled = true;
                            }
                        });
                        state = State.CONNECTING_PRINTER;
                        break;
                    case DISCONNECT_PRINTER:
                        stopNetworkScanner();
                        stopComThread();
                        context.unregisterReceiver(wifiChangeReceiver);
                        state = State.DISCONNECTED;
                        break;
                    default:
                        Log.w(TAG, "unhandled: " + input);
                }
                break;
            case CONNECTING_PRINTER:
                switch (input) {
                    case CONNECTED_TO_PRINTER:
                        state = State.CONNECTED;
                        updateStatus(state, "connected to printer.");
                        startComThread();
                        break;
                    case DROPPED_FROM_NETWORK:
                        state = State.ERRORED;
                        updateStatus(state, "connection failed");
                        break;
                    default:
                        Log.w(TAG, "unhandled: " + input);
                }
                break;
            case CONNECTED:
                switch (input) {
                    case DISCONNECT_PRINTER:
                        stopComThread();
                        unregisterWifiChange();
                        NetworkHelper.disconnectFromPrinter(wifiManager);
                        state = State.DISCONNECTED;
                        updateStatus(state, "disconnected");
                        break;
                    case NETWORK_ERROR:
                    case DROPPED_FROM_NETWORK:
                        unregisterWifiChange();
                        stopComThread();
                        state = State.ERRORED;
                        updateStatus(state, "ERROR, connection lost");
                        break;
                    default:
                        Log.w(TAG, "unhandled: " + input);
                }
                break;
            case DISCONNECTING_PRINTER:
                break;
        }
        Log.d(TAG, "----- state: " + input + ": " + oldState + " --> " + state);
    }

    private void startNetworkScanner() {
        networkScanner = new NetworkScanner(context, PRINTER_SSID, new Runnable() {
            public void run() {
                input(Action.FOUND_PRINTER);
                networkScanner = null;
            }
        });
        networkScanner.start();
    }

    private void stopNetworkScanner() {
        if (networkScanner != null) {
            networkScanner.shutdown();
        }
    }

    private void registerWifiChange() {
        wifiChangeReceiver = new WifiChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiChangeReceiver, intentFilter);
    }

    private void unregisterWifiChange() {
        if (wifiChangeReceiver != null) {
            try {
                context.unregisterReceiver(wifiChangeReceiver);
            }
            catch (IllegalArgumentException e) {
                // ignore
            }
            wifiChangeReceiver = null;
        }
    }

    private void startComThread() {
        if (comThread == null) {
            comThread = new ComThread(new ComThread.ComCallback() {
                @Override
                public void closed() {
                    comThread = null;
                    Log.d(TAG, "connection closed!");
                }
                @Override
                public void failed(String errorMsg) {
                    comThread = null;
                    Log.w(TAG, "com failed: " + errorMsg);
                    input(Action.NETWORK_ERROR);
                }
            });
            comThread.start();
        }
    }

    private void stopComThread() {
        if (comThread != null) {
            comThread.shutDown();
        }
    }

    private void updateStatus(State state, String status) {
        for (StatusCallback callback : callbacks) {
            callback.onStatusChanged(state, status);
        }
    }

    public class WifiChangeReceiver extends BroadcastReceiver {
        public boolean enabled;
        @Override
        public void onReceive(Context c, Intent intent) {
            WifiInfo info = wifiManager.getConnectionInfo();
            NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            ip = NetworkHelper.getLocalIpAddressString();
            Log.d(TAG, "wifi: " + (info != null ? info.getSSID() : "null") + ", state: " + netInfo.getState() + ", " + netInfo.getDetailedState() + ", ip: " + ip);
            if (ip != null && enabled) {
                if (info.getSSID().equals(PRINTER_SSID_QUOTED)) {
                    if (state == State.CONNECTED) {
                        Log.d(TAG, " already connected.");
                    }
                    else if (netInfo.getState() == NetworkInfo.State.CONNECTED &&
                            netInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                        Log.d(TAG, " connected!");
                        input(Action.CONNECTED_TO_PRINTER);
                    }
                    else {
                        Log.d(TAG, "still waiting to connect ...");
                    }
                }
                else {
                    Log.d(TAG, " got ip from wrong network!");
                    input(Action.DROPPED_FROM_NETWORK);
                }
            }
            else if (ip == null && state == State.CONNECTED) {
                Log.d(TAG, "no ip!");
                input(Action.DROPPED_FROM_NETWORK);
            }
        }
    }

}
