package com.tinkerlog.printclient.network;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.tinkerlog.printclient.Const;
import com.tinkerlog.printclient.activity.MainActivity;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkHelper {

    private static final String TAG = "NetworkHelper";

    public static String getLocalIpAddressString() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (!intf.isLoopback() && intf.isUp() && intf.getName().equals("wlan0")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        String sAddr = inetAddress.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (isIPv4) {
                            String ip = inetAddress.getHostAddress().toString();
                            return ip;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "failed", e);
        }
        return null;
    }

    public static boolean isOnPrinterWifi(WifiManager wifiManager) {
        Log.d(TAG, "isOnPrinterWifi: " + wifiManager.getConnectionInfo().getSSID());
        return wifiManager.getConnectionInfo().getSSID().equals(Const.PRINTER_SSID);
    }

    public static void connectToPrinter(final WifiManager wifiManager, final Runnable onEnd) {
        boolean found = false;
        int netId = 0;
        for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
            Log.d(TAG, "-- SSID: " + config.SSID + ", " + config.networkId);
            if (config.SSID.equals(Const.PRINTER_SSID_QUOTED)) {
                netId = config.networkId;
                Log.d(TAG, "found SSID, netId: " + netId);
                found = true;
                break;
            }
        }

        //if (!found) {
        Log.d(TAG, "SSID not found, adding config");
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = Const.PRINTER_SSID_QUOTED;
        // config.preSharedKey = Const.PRINTER_PASS_QUOTED;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        netId = wifiManager.addNetwork(config);
        //}

        final int finalNetId = netId;
        if (netId == -1) {
            Log.w(TAG, "net id -1, failed!");
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "netId: " + finalNetId);
                    boolean b = wifiManager.disconnect();
                    Log.d(TAG, "disconnect: " + b);
                    Thread.sleep(1000);
                    b = wifiManager.enableNetwork(finalNetId, true);
                    Log.d(TAG, "enable: " + b);
                    if (onEnd != null) {
                        onEnd.run();
                    }
                    // wifiChangeReceiver.enabled = true;
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void disconnectFromPrinter(WifiManager wifiManager) {
        Log.d(TAG, "disconnecting from " + Const.PRINTER_SSID);
        boolean b = wifiManager.disconnect();
        Log.d(TAG, "disconnect: " + b);

        for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
            Log.d(TAG, "-- PRINTER_SSID: " + config.SSID + ", " + config.networkId);
            if (config.SSID.equals(Const.PRINTER_SSID_QUOTED)) {
                b = wifiManager.disableNetwork(config.networkId);
                Log.d(TAG, "disable: " + config.networkId + ", " + b);
            }
        }
        b = wifiManager.reconnect();
        Log.d(TAG, "reconnect: " + b);
    }

}
