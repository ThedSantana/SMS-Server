package com.wylder.smsserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by kevin on 5/4/15.
 */
public class ConnectionLogic extends BroadcastReceiver {

    private static boolean connection = false;
    private static ArrayList<OnNetworkChangeListener> listeners = new ArrayList<>();

    /**
     * This is the body method of the broadcast receiver. All intents matching this action are registered
     * In this case, the only intent is coming from ConnectivityManager
     * @param context the application context object of the owner
     * @param intent the data associated with this call
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, 0);
        Log.e("KevinRuntime", "Connection Change network type=" + networkType + " compare to " + ConnectivityManager.TYPE_WIFI);
        if(networkType == ConnectivityManager.TYPE_WIFI) {
            // this will only be called when the wifi state changes.
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if(connection != info.isConnected()){
                // if the network changed connectivity, update the connection and run the listeners
                connection = info.isConnected();
                for (int i = 0; i < listeners.size(); i++) {
                     listeners.get(i).onChange(connection);
                }
            }
        }
    }

    /**
     * This method will determine if the server can start depending on the WiFi state. It must be
     * enabled and connected. An optional Context parameter is used only if this may be the first
     * check of the wifi state. If null, use previous values
     * @param ctx The application's context that can check with certainty for a connection. If null,
     *            previous values of connection will be used.
     */
    public static boolean isConnected(Context ctx){
        if (ctx != null) {
            WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            return manager.getConnectionInfo().getIpAddress() != 0;
        }else {
            return connection;
        }
    }


    public static void addOnNetworkChangeListener(OnNetworkChangeListener listener, Context ctx){
        listeners.add(listener);
        listener.onChange(isConnected(ctx));
    }

    public static void removeOnNetworkChangeListener(OnNetworkChangeListener listener) {
        listeners.remove(listener);
    }

    interface OnNetworkChangeListener{
        /**
         * A listener that is triggered when the phone is/isn't able to run SMS Server.
         * This will be run when the listener is added
         * @param connection true if SMS Server can run
         */
        void onChange(boolean connection);
    }

}
