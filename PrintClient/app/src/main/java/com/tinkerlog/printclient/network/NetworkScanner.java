package com.tinkerlog.printclient.network;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

/**
 * Created by alex on 11.08.15.
 */
public class NetworkScanner extends Thread {

    private static final String TAG = "NetworkScanner";

    private WifiManager wifiManager;
    private String ssid;
    private Runnable onFound;
    private boolean running = true;

    public NetworkScanner(Context context, String ssid, Runnable onFound) {
        wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        this.ssid = ssid;
        this.onFound = onFound;
    }

    public void shutdown() {
        running = false;
    }

    public void run() {
        while (running) {
            try {
                Log.d(TAG, "scanning networks ...");
                List<ScanResult> scan = wifiManager.getScanResults();
                for (ScanResult result : scan) {
                    if (result.SSID.equals(ssid)) {
                        Log.d(TAG, "found " + ssid);
                        if (onFound != null) {
                            onFound.run();
                        }
                        running = false;
                        break;
                    }
                }
                if (running) {
                    Thread.sleep(1000);
                }
            }
            catch (InterruptedException e) {
                // ignored
            }
        }
        Log.d(TAG, "scanner going down");
    }

}

