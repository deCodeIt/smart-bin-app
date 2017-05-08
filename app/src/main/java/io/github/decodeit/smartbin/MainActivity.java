package io.github.decodeit.smartbin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public static WifiService wifiService;
    public static soundHandler sH;
    public static MagnetService magnetService;

    public static final String WIFI_TAG = "WIFI";
    public static final String SOUND_TAG = "SOUND";
    public static final String MAGNET_TAG = "MAGNET";
    public static final String TAG = "SB";

    private boolean processingWifi = false;

    public static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x12345;
    public static final int PERMISSIONS_REQUEST_CODE_RECORD_AUDIO = 0x12346;
    public static final int PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 0x12347;
    public static DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get permission for Android versions >= Marshmallow (6.0)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MainActivity.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
                //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
            }
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions( new String[]{Manifest.permission.RECORD_AUDIO},
                        MainActivity.PERMISSIONS_REQUEST_CODE_RECORD_AUDIO);
            }
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions( new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MainActivity.PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }
        }

        db = new DBHelper(this); // get an instance of DBHelper
        wifiService = new WifiService(this); // Instantiate WifiService Object for future use
        sH = new soundHandler(this);    // soundhandler object
        magnetService = new MagnetService(this);
        initializeWifi();
        soundHandling();
        magneticHandling();

        Button upload = (Button) findViewById(R.id.btn_upload);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename1 = "sound_data.wav";
                String FULL_FILE_PATH1 = MainActivity.db.getSoundStorageDir() + File.separator + filename1;
                Log.d("File Path1", FULL_FILE_PATH1);
                String filename2 = "wifi_data.txt";
                String FULL_FILE_PATH2 = MainActivity.db.getWifiStorageDir() + File.separator + filename2;
                Log.d("File Path2", FULL_FILE_PATH2);

                sH.uploadMultipleFiles(FULL_FILE_PATH1,FULL_FILE_PATH2);
                //sH.uploadFile(FULL_FILE_PATH2);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
        } else if (requestCode == PERMISSIONS_REQUEST_CODE_RECORD_AUDIO) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
        } else if (requestCode == PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
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
        magnetService.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        wifiService.deRegister(); // stops updating wifi signal strength
        magnetService.deRegister();

        // added by npk
        // stop recording
//        sH.stopAudioRecord();
    }

    @Override
    protected void onDestroy() {
        wifiService.disableHotspot();
        sH.deRegister();
        super.onDestroy();
    }

    public void setEnabledButton(final Button button,final boolean enable){
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
                findViewById(R.id.sound_start).setEnabled(false); // disable button that requests server to play the audio file
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

    public void soundHandling(){

        final Button play = (Button) findViewById(R.id.sound_start);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiService.hasConnection()){
                    play.setEnabled(false);
                    long startTime = System.currentTimeMillis()+sH.SOUND_START_DELAY;
                    String fileName = "sine_wave.wav";
                    wifiService.runMessageClient(fileName,startTime,sH.getDuration(fileName),false);
                    sH.setDelayedRecordingService(startTime,sH.getDuration(fileName));
                } else {
                    play.setEnabled(true);
                    Log.d(MainActivity.SOUND_TAG, "Not yet connected to Wifi");
                }
            }
        });
    }

    private void magneticHandling(){
        final Button start = (Button) findViewById(R.id.MF_start);
//        final Button stop = (Button) findViewById(R.id.MF_stop);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setEnabled(false);
//                stop.setEnabled(true);
                magnetService.start();
            }
        });

//        stop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                stop.setEnabled(false);
//                start.setEnabled(true);
//                magnetService.stop();
//            }
//        });
    }

}
