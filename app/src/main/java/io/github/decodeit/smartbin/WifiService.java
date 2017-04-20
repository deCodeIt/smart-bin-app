package io.github.decodeit.smartbin;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;

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
    private static final String ssid = "i_am_smart_bin"; // Hotspot SSID
    private static final String passkey = "iloveindia"; // Hotspot Password
//    private static final String ssid = "$@UR@B#"; // Hotspot SSID
//    private static final String passkey = "cuteassfuck"; // Hotspot Password
//    private static final String ssid = "LifeHacker"; // Hotspot SSID
//    private static final String passkey = "getlost@123"; // Hotspot Password
    private boolean isCollectingSamples = false;
//    private static final float SAMPLES_PER_SECOND = 2.0f;
    private static final float MAX_SAMPLES = 5.0f; // samples to be collected
    private float NUM_SAMPLES; // current number of samples collected
    private Thread t;
    private BroadcastReceiver isConnected;
    // update wifi signal strength
    private BroadcastReceiver broadcastReceiver;
    private ArrayList<Integer> signalHistory; // store the received signals
    private long prevTimestamp;
    private static final long TIME_DIFFERENCE = 500L;
    private boolean isLinked = false; // tells whether the client is connected or not

    // constructor
    WifiService(Activity mActivity){
        // ensure that the hotspot is stopped before Running the app
        this.activity = mActivity;
        prevTimestamp = 0L;
        // signal strength recorded will be stored over here
        signalHistory = new ArrayList<Integer>((int)MAX_SAMPLES);
        for(int _i=0;_i<MAX_SAMPLES; _i++) {
            signalHistory.add(null);
        }

        isConnected = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = conMan.getActiveNetworkInfo();
                if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI && wifiManager!=null && wifiManager.getConnectionInfo().getSSID().equals("\""+ssid+"\"")) {
                    Log.d("WifiReceiver", "Have Wifi Connection");
                    getIpAddress();
                    getServerIpAddress();
                    isLinked = true;
//                    runMessageClient(); // runs the message client
                }
                else {
                    Log.d("WifiReceiver", "Don't have Wifi Connection");
                }
            }
        };

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
//                Log.d(MainActivity.WIFI_TAG, "Scan results available");
                    List<ScanResult> sr = wifiManager.getScanResults();
                    if (isCollectingSamples) {
                        if(sr.size() > 0) {
                            Log.d(MainActivity.WIFI_TAG, sr.get(0).timestamp + "");
                            if (NUM_SAMPLES < MAX_SAMPLES && new Date().getTime() - prevTimestamp > TIME_DIFFERENCE) {
                                prevTimestamp = new Date().getTime();
                                signalHistory.set((int)NUM_SAMPLES,wifiManager.getConnectionInfo().getRssi());
                                NUM_SAMPLES++;
                                Log.d(MainActivity.WIFI_TAG, "#"+((int)NUM_SAMPLES)+": "+wifiManager.getConnectionInfo().getRssi() + " dBm");
                                signalStrengthTextView.setText("#"+((int)NUM_SAMPLES)+": "+wifiManager.getConnectionInfo().getRssi() + " dBm");
                            } else {
                                Log.d(MainActivity.WIFI_TAG, "Instant Result, noise");
                            }
                        } else {
                            Log.d(MainActivity.WIFI_TAG, "Empty Scan Results");
                        }
                        // start a fresh scan for next update of signal strength
                        if(NUM_SAMPLES < MAX_SAMPLES) {
                            wifiManager.startScan();
                        } else {
                            stopCollectingSamples();
                        }
                    }
                }
            }
        };

        syncToken = new Object();
        netConfig = new WifiConfiguration();
        signalStrengthTextView = (TextView) activity.findViewById(R.id.client_signal);


        wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        while( !wifiManager.isWifiEnabled() ){
            // turn on the wifi if not
            wifiManager.setWifiEnabled(true);
            SystemClock.sleep(500);
        }
        setUpWifiClient();
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
                Log.d(MainActivity.WIFI_TAG, "Found Configured Wifi at id: " + i.networkId);
//                break;
            }
        }
    }

    public void startCollectingSamples(){
        NUM_SAMPLES = 0;
        isCollectingSamples = true;
//        scanNow(); // initiate scanning
        wifiManager.startScan();
    }

    public void stopCollectingSamples(){
        isCollectingSamples = false;
        signalStrengthTextView.setText(R.string.network_down);
        if(NUM_SAMPLES == MAX_SAMPLES){
            // store the data in table
            MainActivity.db.insertWifiData(signalHistory);
        }
        activity.findViewById(R.id.client_start).setEnabled(true);
        activity.findViewById(R.id.client_stop).setEnabled(false);
    }

    public void register(){
        Log.d(MainActivity.WIFI_TAG,"In onRegister");
//        activity.registerReceiver(rssiChangeReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        if(broadcastReceiver!=null)
            activity.getApplicationContext().registerReceiver(broadcastReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if(isConnected!=null)
            activity.getApplicationContext().registerReceiver(isConnected, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void deRegister(){
//        activity.unregisterReceiver(rssiChangeReceiver);
        if(broadcastReceiver!=null)
            activity.getApplicationContext().unregisterReceiver(broadcastReceiver);
        if(isConnected!=null)
            activity.getApplicationContext().unregisterReceiver(isConnected);
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
        isLinked = false;
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

    public boolean hasConnection(){
        return isLinked;
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
        new Thread(r).start();
    }

    public void runMessageClient(final String fileName, final long startTime, final long duration, final boolean isLast){
        // runs the client and sends message to server
        if(isLinked) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    MessageClient m = new MessageClient(activity, getServerIpAddress());
                    m.createMessage(fileName, startTime, duration, isLast);
                    m.setUp();
                }
            };
            new Thread(r).start();
        } else {
            Log.d(MainActivity.SOUND_TAG, "Not yet connected to hotspot");
        }
    }

}
