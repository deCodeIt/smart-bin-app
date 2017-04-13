package io.github.decodeit.smartbin;

import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private WifiService wifiService;
    public static final String WIFI_TAG = "WIFI";
    public static final String TAG = "SB";
    private boolean processingWifi = false;
    public static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x12345;
    public static DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DBHelper(this); // get an instance of DBHelper
        wifiService = new WifiService(this); // Instantiate WifiService Object for future use
        initializeWifi();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MainActivity.TAG,"In onResume");
        wifiService.register(); // resumes updating wifi signal strength
    }

    @Override
    protected void onPause() {
        super.onPause();
        wifiService.deRegister(); // stops updating wifi signal strength
    }

    @Override
    protected void onDestroy() {
        wifiService.disableHotspot();
        super.onDestroy();
    }

    private void setEnabledButton(final Button button,final boolean enable){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setEnabled(enable);
            }
        });
    }

    public void initializeWifi(){

        // hotspot button clicked
        final Button wifiHotspot = (Button) findViewById(R.id.wifi_hotspot);
        final Button wifiClient = (Button) findViewById(R.id.wifi_client);
        final Button clientStart = (Button) findViewById(R.id.client_connect);
        final Button clientStop = (Button) findViewById(R.id.client_disconnect);
        final Button hotspotStart = (Button) findViewById(R.id.hotspot_start);
        final Button hotspotStop = (Button) findViewById(R.id.hotspot_stop);
        final Button clientCollectSamples = (Button) findViewById(R.id.client_start);
        final Button clientNotCollectSamples = (Button) findViewById(R.id.client_stop);


        wifiHotspot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.wifi_options).setVisibility(View.GONE);
                findViewById(R.id.hotspot_options).setVisibility(View.VISIBLE);
            }
        });

        // client button clicked
        wifiClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.wifi_options).setVisibility(View.GONE);
                findViewById(R.id.client_options).setVisibility(View.VISIBLE);
            }
        });

        // client start recording samples
        clientStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientStart.setEnabled(false);
                clientNotCollectSamples.setEnabled(false);
                wifiService.connect();
                clientStop.setEnabled(true);
                clientCollectSamples.setEnabled(true);
            }
        });

        // client stop recording samples
        clientStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientStop.setEnabled(false);
                clientNotCollectSamples.setEnabled(false);
                wifiService.disconnect();
                clientCollectSamples.setEnabled(false);
                clientStart.setEnabled(true);
            }
        });

        clientCollectSamples.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientCollectSamples.setEnabled(false);
                wifiService.startCollectingSamples();
                clientNotCollectSamples.setEnabled(true);
            }
        });

        clientNotCollectSamples.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiService.stopCollectingSamples();
                clientCollectSamples.setEnabled(true);
                clientNotCollectSamples.setEnabled(false);
            }
        });



        // Start Hotspot
        hotspotStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        setEnabledButton(hotspotStart,false);
//                        hotspotStart.setEnabled(false);
                        wifiService.enableHotspot();
                        setEnabledButton(hotspotStop,true);
//                        synchronized (wifiService.getToken()){
//                            try {
//                                wifiService.getToken().wait();
//                                setEnabledButton(hotspotStop,true);
////                                hotspotStop.setEnabled(true);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                                Log.d(MainActivity.WIFI_TAG,"Error Disabling Hotspot");
//                                setEnabledButton(hotspotStart,true);
////                                hotspotStart.setEnabled(true);
//                            }
//                        }
                    }
                };
                Thread runnableThread = new Thread(runnable);
                runnableThread.start();
            }
        });

        // Stop Hotspot
        hotspotStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        setEnabledButton(hotspotStop,false);
//                        hotspotStop.setEnabled(false);
                        wifiService.disableHotspot();
                        setEnabledButton(hotspotStart,true);
//                        synchronized (wifiService.getToken()){
//                            try {
//                                wifiService.getToken().wait();
//                                setEnabledButton(hotspotStart,true);
////                                hotspotStart.setEnabled(true);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                                Log.d(MainActivity.WIFI_TAG,"Error Disabling Hotspot");
//                                setEnabledButton(hotspotStop,true);
////                                hotspotStop.setEnabled(true);
//                            }
//                        }
                    }
                };
                Thread runnableThread = new Thread(runnable);
                runnableThread.start();

            }
        });

    }
}
