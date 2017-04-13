package io.github.decodeit.smartbin;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Created by laststnd on 7/4/17.
 */

public class WifiService {

    private Activity activity;
    private WifiManager wifiManager;
    private WifiConfiguration netConfig;
    private Object syncToken;
    private int netId;
    private TextView signalStrengthTextView;
//    private int currentRSSI;
//    private static final String ssid = "i_am_smart_bin"; // Hotspot SSID
//    private static final String passkey = "iloveindia"; // Hotspot Password
    private static final String ssid = "$@UR@B#"; // Hotspot SSID
    private static final String passkey = "cuteassfuck"; // Hotspot Password
    private boolean isCollectingSamples = false;
//    private static final float SAMPLES_PER_SECOND = 2.0f;
    private static final float MAX_SAMPLES = 10; // samples to be collected
    private float NUM_SAMPLES; // current number of samples collected

    private BroadcastReceiver isConnected = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI && wifiManager!=null && wifiManager.getConnectionInfo().getSSID().equals("\""+ssid+"\"")) {
                Log.d("WifiReceiver", "Have Wifi Connection");
                getIpAddress();
                getServerIpAddress();
//                runMessageClient();
            }
            else
                Log.d("WifiReceiver", "Don't have Wifi Connection");
        }
    };


//    private Runnable getWifiSignal = new Runnable() {
//        @Override
//        public void run() {
//            while(true) {
//                try {
////                    Log.d(MainActivity.WIFI_TAG, "getWifiSignal");
//                    if (isCollectingSamples) {
//                        Log.d(MainActivity.WIFI_TAG, wifiManager.getConnectionInfo().getRssi() + " dBm");
//                        activity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                signalStrengthTextView.setText(wifiManager.getConnectionInfo().getRssi() + " dBm");
//                            }
//                        });
//                    }
//                    Thread.sleep(Math.round(1000.0f / SAMPLES_PER_SECOND));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    };


//    private BroadcastReceiver rssiChangeReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            currentRSSI = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI,0);
//        }
//    };

    // update wifi signal strength
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
//                Log.d(MainActivity.WIFI_TAG, "Scan results available");
                if (isCollectingSamples){
                    if(NUM_SAMPLES < MAX_SAMPLES) {
                        NUM_SAMPLES++;
                        Log.d(MainActivity.WIFI_TAG, wifiManager.getConnectionInfo().getRssi() + " dBm");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                signalStrengthTextView.setText(wifiManager.getConnectionInfo().getRssi() + " dBm");
                            }
                        });
                    } else {
                        stopCollectingSamples();
                    }

                    // start a fresh scan for next update of signal strength
                    scanNow();
                }
            }
        }
    };

    // constructor
    WifiService(Activity mActivity){
        // ensure that the hotspot is stopped before Running the app
        this.activity = mActivity;
        syncToken = new Object();
        netConfig = new WifiConfiguration();
        signalStrengthTextView = (TextView) activity.findViewById(R.id.client_signal);

        wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        while( !wifiManager.isWifiEnabled() ){
            // turn on the wifi if not
            wifiManager.setWifiEnabled(true);
        }
//        wifiManager.disconnect();

        setUpWifiClient();

//        Thread wifiSignalScanner = new Thread(getWifiSignal);
//        wifiSignalScanner.start();
    }

    private final Runnable mStartScan = new Runnable() {
        @Override
        public void run() {
            wifiManager.startScan();
//            Log.d(MainActivity.WIFI_TAG,"Scanning...");
        }
    };

    private void scanNow(){
        // scan for change in wifi signals
        Thread t = new Thread(mStartScan);
        t.start();
    }

    private void setUpWifiClient(){
        // hotspot configuration setup so that we can connect to it later
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ssid + "\"";
        conf.preSharedKey = "\""+ passkey +"\"";

        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            wifiManager.disableNetwork(i.networkId);
            if(i.SSID != null && i.SSID.equals("\""+ssid+"\"")) {
                netId = i.networkId;
                Log.d(MainActivity.WIFI_TAG, "Found Configured Wifi");
//                break;
            }
        }
    }

    public void startCollectingSamples(){
        NUM_SAMPLES = 0;
        isCollectingSamples = true;
        scanNow(); // initiate scanning
    }

    public void stopCollectingSamples(){
        isCollectingSamples = false;
        signalStrengthTextView.setText(R.string.network_down);
        activity.findViewById(R.id.client_start).setEnabled(true);
        activity.findViewById(R.id.client_stop).setEnabled(false);
    }

    public void register(){
//        activity.registerReceiver(rssiChangeReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        activity.registerReceiver(broadcastReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        activity.registerReceiver(isConnected, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void deRegister(){
//        activity.unregisterReceiver(rssiChangeReceiver);
        activity.unregisterReceiver(broadcastReceiver);
        activity.unregisterReceiver(isConnected);
    }

    public void connect(){
        // clients connects to a wifi network
        Log.d(MainActivity.WIFI_TAG, "Connecting...");
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        Log.d(MainActivity.WIFI_TAG, "Connected");
//        getIpAddress();
//        startCollectingSamples(); // collect wifi Signal strength samples
    }

    public void disconnect(){
        // clients disconnects from a wifi network
        wifiManager.disableNetwork(netId);
        wifiManager.disconnect();
        signalStrengthTextView.setText(R.string.network_down);
        Log.d(MainActivity.WIFI_TAG, "Disconnected");
        stopCollectingSamples(); // stop collecting wifi signal strength samples
    }

    // enable the wifiHotspot
    public void enableHotspot(){

        if(wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(false);
        }

        netConfig.SSID = ssid;
        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        netConfig.preSharedKey = passkey;

        try{
            Method setWifiApMethod = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean apstatus=(Boolean) setWifiApMethod.invoke(wifiManager, netConfig,true);
            Method isWifiApEnabledmethod = wifiManager.getClass().getMethod("isWifiApEnabled");
            while(!(Boolean)isWifiApEnabledmethod.invoke(wifiManager)){Thread.sleep(1000);};
            Method getWifiApStateMethod = wifiManager.getClass().getMethod("getWifiApState");
            int apstate=(Integer)getWifiApStateMethod.invoke(wifiManager);
            Method getWifiApConfigurationMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            netConfig=(WifiConfiguration)getWifiApConfigurationMethod.invoke(wifiManager);
//            synchronized (syncToken){
//                syncToken.notify();
//            }
            Log.d("CLIENT", "\nSSID:"+netConfig.SSID+"\nPassword:"+netConfig.preSharedKey+"\n");
            makeToast("Wifi Hotspot Enabled", Toast.LENGTH_SHORT);
            runMessageServer();
//            Toast.makeText(activity,"Wifi Hotspot Enabled",Toast.LENGTH_SHORT).show();


        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
        }
    }

    public void disableHotspot(){

        try {
            Method setWifiApMethod = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean apstatus=(Boolean) setWifiApMethod.invoke(wifiManager, netConfig, false); // disable wifi
            Method isWifiApEnabledmethod = wifiManager.getClass().getMethod("isWifiApEnabled");
            while((Boolean)isWifiApEnabledmethod.invoke(wifiManager)){Thread.sleep(1000);};
//            synchronized (syncToken){
//                syncToken.notify();
//            }
            Log.d(MainActivity.WIFI_TAG,"Wifi Disabled");
            makeToast("Wifi Hotspot Disabled", Toast.LENGTH_SHORT);

//            Toast.makeText(activity,"Wifi Hotspot Disabled",Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
        }
    }

    private void makeToast(final String text, final int duration){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity,text,duration).show();
            }
        });
    }

    public String getIpAddress(){
        // returns the ip address of client
        String ipAddressString;
        if(wifiManager!=null) {
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }

//            Log.d(MainActivity.WIFI_TAG,"IP int: "+ipAddress);

            byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

            try {
                ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            } catch (UnknownHostException ex) {
                Log.e("WIFIIP", "Unable to get host address.");
                ipAddressString = null;
            }
        } else {
            ipAddressString = null;
        }
        Log.d(MainActivity.WIFI_TAG, "Ip address: " + ipAddressString);
        return ipAddressString;
    }

    public String getServerIpAddress(){
        // returns the ip address of connected wifi network
        String ipAddressString;
        if(wifiManager!=null) {
            int ipAddress = wifiManager.getDhcpInfo().serverAddress;
            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }

//            Log.d(MainActivity.WIFI_TAG,"IP server int: "+ipAddress);

            byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

            try {
                ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            } catch (UnknownHostException ex) {
                Log.e("WIFIIP", "Unable to get Server address.");
                ipAddressString = null;
            }
        } else {
            ipAddressString = null;
        }
        Log.d(MainActivity.WIFI_TAG, "Server Ip address: " + ipAddressString);
        return ipAddressString;
    }

    public Object getToken(){
        return syncToken;
    }

    private void runMessageServer(){
        // sets up the server to receive messages from server
        Runnable r = new Runnable() {
            @Override
            public void run() {
                MessageServer m = new MessageServer(activity);
                m.setUp();
            }
        };
        Thread rT = new Thread(r);
        rT.start();
    }

    private void runMessageClient(){
        // runs the client and sends message to server
        Runnable r = new Runnable() {
            @Override
            public void run() {
                MessageClient m = new MessageClient(activity,getServerIpAddress());
                m.createMessage(123,12,false);
                m.setUp();
            }
        };
        Thread rT = new Thread(r);
        rT.start();
    }

}
