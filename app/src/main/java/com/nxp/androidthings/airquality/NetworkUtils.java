package com.nxp.androidthings.airquality;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by b07211 on 12/26/16.
 */

public class NetworkUtils {
    public static final String WIFI_SETUP_PACKAGE = "com.google.wifisetup";
    public static final String WIFI_SETUP_SERVICE = "com.google.wifisetup.WifiSetupService";
    public static final String TAG = "NetworkUtils";
    private ConnectivityManager mConnectivityManager;
    private SocketThread socketThread;


    public NetworkUtils(ConnectivityManager connectivityManager) {
        mConnectivityManager = connectivityManager;
    }

    public boolean getCurrentNetworkState() {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        if(info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "Current network1：" + name);
            return true;
        } else {
            return false;
        }
    }

    public void connectServer(String ip, int port) {
        socketThread = new SocketThread(ip, port);
        socketThread.start();
    }

    public void sendData(int PM2_5, int PM10) {
        String data = "SET:" + Integer.toString(PM2_5)+Integer.toString(PM10);
        Log.i(TAG, "SendData: " + data);
        socketThread.out.println(data);
    }

    class SocketThread extends Thread {
        private String ip;
        private int port;
        private volatile Socket socket;
        private PrintWriter out;

        public SocketThread(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(ip, port);
                out = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(
                                socket.getOutputStream())), true);
                sleep(3000);

                if (out != null) {
                    Log.i(TAG, "Socket is ready!");
                } else {
                    Log.w(TAG, "out is null");
                }
            } catch (Exception e) {
                Log.w(TAG, "new Socket error!");
                e.printStackTrace();
            }
        }
    }

    /*
    adb shell am startservice \
      -n com.google.wifisetup/.WifiSetupService \
      -a WifiSetupService.Connect \
      -e ssid wifi-ap-name \
      -e passphrase pwd


     */
    static public boolean setupWifiConnection(Context context, String wifiSSID, String password) {
        Intent serviceIntent = new Intent();

        Log.i(TAG, " " + wifiSSID.length() + ' ' + password.length());
        serviceIntent.setClassName(WIFI_SETUP_PACKAGE, WIFI_SETUP_SERVICE);
        serviceIntent.setAction("WifiSetupService.Connect");
        serviceIntent.putExtra("ssid", wifiSSID);
        //serviceIntent.putExtra("ssid", "zczasq");
        if(password != null)
            serviceIntent.putExtra("passphrase", password);
            //serviceIntent.putExtra("passphrase", "1234567890");
        Log.i(TAG, "setupWifiConnection1:" + wifiSSID);
        context.startService(serviceIntent);

        return true;
    }

    /*
      adb shell am startservice \
      -n com.google.wifisetup/.WifiSetupService \
      -a WifiSetupService.Reset
     */
    static public boolean resetWifiConnection(Context context) {
        Intent serviceIntent = new Intent();

        serviceIntent.setClassName(WIFI_SETUP_PACKAGE, WIFI_SETUP_SERVICE);
        serviceIntent.setAction("WifiSetupService.Reset");

        context.startService(serviceIntent);

        return true;
    }
}
