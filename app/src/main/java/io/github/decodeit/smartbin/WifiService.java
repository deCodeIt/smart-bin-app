package io.github.decodeit.smartbin;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
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
    private static final String ssid = "LifeHacker"; // Hotspot SSID
    private static final String passkey = "getlost@123"; // Hotspot Password

    // constructor
    WifiService(Activity mActivity){
        // ensure that the hotspot is stopped before Running the app
        this.activity = mActivity;
        syncToken = new Object();
        netConfig = new WifiConfiguration();

        wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        while( !wifiManager.isWifiEnabled() ){
            // turn on the wifi if not
            wifiManager.setWifiEnabled(true);
        }
        wifiManager.disconnect();

        setUpWifiClient();
    }

    // connect to wifiHotspot
    private void setUpWifiClient(){

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ssid + "\"";
        conf.preSharedKey = "\""+ passkey +"\"";

        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\""+ssid+"\"")) {
                netId = i.networkId;
                Log.d(MainActivity.WIFI_TAG, "Found Configured Wifi");
                break;
            }
        }
    }
    public void connect(){
        // clients connects to a wifi network
        Log.d(MainActivity.WIFI_TAG, "Connecting...");
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        Log.d(MainActivity.WIFI_TAG, "Connected");
    }

    public void disconnect(){
        // clients disconnects from a wifi network
        wifiManager.disconnect();
        Log.d(MainActivity.WIFI_TAG, "Disconnected");
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

    public Object getToken(){
        return syncToken;
    }

}
