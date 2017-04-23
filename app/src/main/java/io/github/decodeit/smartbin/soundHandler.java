package io.github.decodeit.smartbin;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by prince on 7/4/17.
 */

public class soundHandler {

    private Activity activity;
    public  MediaPlayer mediaPlayer;
    public  MediaRecorder recorder;
    //String record_file = "";
    public  File audiofile = null;
    public final int sampleRate = 22050;
    public AudioRecord audio;
    public int bufferSize;
    public double lastLevel = 0;
    public Thread thread;
    public final int SAMPLE_DELAY = 1;  // in ms
    public long duration = 0; // time in millisecond after which have to stop recording
    public ArrayList<Short> amps;
    private int loopVar, bufferReadResult;
    private short[] buffer;
    private  ExecutorService threadPool;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntentServer, pendingIntentClient;
    private final static String ALARM_INTENT_SERVER = "alarmIntentServer";
    private final static String ALARM_INTENT_CLIENT = "alarmIntentClient";
    public boolean isRecording = false;
    private boolean isPlaying = false;
    private long test_count = 0;
    ProgressDialog progressDialog;
    public static final long SOUND_START_DELAY = 5000; // milliseconds
    private static final int FILE_SELECT_CODE = 0;
    public ArrayList<String> mediaPaths = new ArrayList<String>();
//    private SharedPreferences settings;
//    private final static String SHARED_PREFS = "SmartBinPrefs";


    private BroadcastReceiver soundReceiver,soundRecorder;

    // constructor
    soundHandler(Activity act){
        this.activity = act;
        isRecording = false;
        amps = new ArrayList<Short>();
        soundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // plays media on receiving intent at specified time
                Log.d(MainActivity.SOUND_TAG, "Playing media file");
                playSound();
//                Toast.makeText(activity, "GOT INTENT", Toast.LENGTH_SHORT).show();
            }
        };

        soundRecorder = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isRecording = true;
                Log.d(MainActivity.SOUND_TAG, "RECORDING media file");

                // stop recording after the specified duration (media file duration)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopAudioRecord();
                        duration = 0;
                    }
                }, duration);
            }
        };
        alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        register();

        // set progress dialog
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Uploading...");

    }

    public void setDelayedRecordingService(long startTime, long duration){
        this.duration = duration;
        // starts recording media on server after synchronizing with the received message
        if(!isRecording){
//            isRecording = true;
            // set recording to start at this time
            //prepareSound(message.getFileName());
            alarmManager.setExact(AlarmManager.RTC, startTime, pendingIntentClient);
            initialiseAudioRecorder();
        } else {
            Log.d(MainActivity.SOUND_TAG, "Already Recording!");
        }
    }

//    public void setDelayedRecordingService(long startTime, final long duration){
//        if(!isRecording){
//            this.duration = duration;
//            // starts recording media on server after synchronizing with the received message
//            isRecording = false;
//            // set recording to start at this time
////            alarmManager.setExact(AlarmManager.RTC, startTime, pendingIntentClient);
//            initialiseAudioRecorder();
//
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    isRecording = true;
//                    Log.d(MainActivity.SOUND_TAG, "RECORDING media file");
//                    // stop recording after the specified duration (media file duration)
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            stopAudioRecord();
//                        }
//                    }, duration);
//                }
//            },startTime-System.currentTimeMillis());
//
//        } else {
//            Log.d(MainActivity.SOUND_TAG, "Already Recording!");
//        }
//    }

    public void setDelayedPlayingService(Message message) {
        // starts playing media on server after synchronizing with the received message
        if(!isPlaying){
            if(message.getStartTime() > System.currentTimeMillis()) {
                isPlaying = true;
                // set recording to start at this time
                prepareSound(message.getFileName());
                alarmManager.setExact(AlarmManager.RTC, message.getStartTime(), pendingIntentServer);
            } else {
                Log.d(MainActivity.SOUND_TAG, "Received Message after start time has passed");
            }

        } else {
            Log.d(MainActivity.SOUND_TAG, "Already Playing!");
        }
    }

    public long getDuration(String fileName) {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            AssetFileDescriptor afd = activity.getAssets().openFd(fileName);
            mediaMetadataRetriever.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            mediaMetadataRetriever.release();
            Log.d(MainActivity.SOUND_TAG, "Duration: "+durationStr);
            return Long.parseLong(durationStr);
        } catch (IOException e) {
            Log.d(MainActivity.SOUND_TAG, "Error getting file Duration");
            return -1L;
        }
    }

    public void deRegister(){
        if(alarmManager!=null && pendingIntentServer !=null) {
            alarmManager.cancel(pendingIntentServer);
        }
        activity.unregisterReceiver(soundReceiver);

        if(alarmManager!=null && pendingIntentClient !=null) {
            alarmManager.cancel(pendingIntentClient);
        }
        activity.unregisterReceiver(soundRecorder);
    }

    public void register(){
        activity.registerReceiver(soundReceiver,new IntentFilter(ALARM_INTENT_SERVER));
        pendingIntentServer = PendingIntent.getBroadcast(activity,0,new Intent(ALARM_INTENT_SERVER),PendingIntent.FLAG_UPDATE_CURRENT);
        activity.registerReceiver(soundRecorder,new IntentFilter(ALARM_INTENT_CLIENT));
        pendingIntentClient = PendingIntent.getBroadcast(activity,0,new Intent(ALARM_INTENT_CLIENT),PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public void prepareSound(String fileName){
        // fileName = "sine_wave.wav"
        try {
            AssetFileDescriptor afd = activity.getAssets().openFd(fileName);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.setOnCompletionListener(onCompletionListener);
            Log.d(MainActivity.SOUND_TAG, "Media Prepared");
        } catch (IOException e) {
            isPlaying = false;
            Log.d(MainActivity.SOUND_TAG, "Cannot Open File to play");
            e.printStackTrace();
        }
    }

    public void playSound(){
        // plays the audio file on server
        if(mediaPlayer != null) {
            Log.d(MainActivity.SOUND_TAG, "Playing Begins");
            mediaPlayer.start();
        } else {
            isPlaying = false;
            Log.d(MainActivity.SOUND_TAG,"MediaPlayer isn't initialized");
        }
    }

    public MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            mp.stop();
            mp.release();
            Log.e("Stopping", "Media Player");
            mp = null;
            mediaPlayer = null;
            isPlaying = false;
        }
    };

    public void initialiseAudioRecorder(){
        //sampleRate = 8000;
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            buffer = new short[bufferSize];
            Log.d(MainActivity.SOUND_TAG,"InInitializeAudioRecorder, bufferSize: "+bufferSize);

            final Runnable audioReadRunnable = new Runnable() {
                @Override
                public void run() {
//                    Log.d(MainActivity.SOUND_TAG, "Read from buffer");
                    readAudioBuffer();
                }
            };

            threadPool = Executors.newFixedThreadPool(1);

            audio.setPositionNotificationPeriod(bufferSize/4);
            audio.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioRecord audioRecord) {
                    //not needed yet
                    Log.d(MainActivity.SOUND_TAG,"onMarkerReached");
                }

                @Override
                public void onPeriodicNotification(AudioRecord audioRecord) {
//                    new Thread(audioReadRunnable).start();
//                    Log.d(MainActivity.SOUND_TAG, "Received Periodic Notification"+test_count++);
                    try {
//                        Log.d(MainActivity.TAG, "Recording");
                        threadPool.execute(audioReadRunnable);
                    } catch (java.util.concurrent.RejectedExecutionException e) {
                        Log.d(MainActivity.SOUND_TAG, "Thread Terminated");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                      threadPool.shutdown();
                }
            });

            Log.e("Initialize", "audioRecord Initialized");
            audio.startRecording();
        } catch (Exception e) {
            android.util.Log.e("TrackingFlow", "Exception", e);
        }
    }

    // function to get output
    public void readAudioBuffer() {

        try {
            if (audio != null) {
                // Sense the voice…
                buffer = new short[bufferSize];
                bufferReadResult = audio.read(buffer, 0, bufferSize);
                if(isRecording) {
                    for (loopVar = 0; loopVar < bufferReadResult; loopVar++) {
                        amps.add(buffer[loopVar]);
                    }
                }
            } else {
                Log.d(MainActivity.SOUND_TAG, "Audio is Null");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAudioRecord(){
        Log.d(MainActivity.SOUND_TAG, "Terminating Recording");
        duration = 0;
        isRecording = false;
        if(thread != null && !thread.isInterrupted()){
            thread.interrupt();
            thread = null;
        }
        if(threadPool!=null && !threadPool.isShutdown()){
            threadPool.shutdownNow();
        }
        try {
            if (audio != null) {
                audio.stop();
                audio.release();
                Log.e("StopRecord", "audioRecord stop");
                audio = null;
                String amp = amps.toString();
                Log.e("amplitudes", amp);
                Log.e("Size", ""+amps.size());
                rawDataToWavFile(null); // null causes the desired file labelling
                amps = new ArrayList<Short>(); // make space for new array read
                activity.findViewById(R.id.sound_start).setEnabled(true); // enable the button to record another file
            }
        } catch (Exception e) {e.printStackTrace();}


        //drawfigure();

    }

    public void rawDataToWavFile(final String outFileName) throws IOException {
        short[] tmp_buf = new short[amps.size()];
        for (loopVar=0;loopVar < tmp_buf.length;loopVar++) {
            tmp_buf[loopVar] = amps.get(loopVar);
        }
        Wave w = new Wave(activity,sampleRate,(short)1,tmp_buf,0,amps.size()-1);
        w.wroteToFile(outFileName);
    }


    public void uploadMultipleFiles(String mediaPath1,String mediaPath2, String mediaPath3) {
        progressDialog.show();

        // Map is used to multipart the file using okhttp3.RequestBody
        File file1 = new File(mediaPath1);
        File file2 = new File(mediaPath2);
        File file3 = new File(mediaPath3);

        // Parsing any Media type file
        RequestBody requestBody1 = RequestBody.create(MediaType.parse("*/*"), file1);
        RequestBody requestBody2 = RequestBody.create(MediaType.parse("*/*"), file2);
        RequestBody requestBody3 = RequestBody.create(MediaType.parse("*/*"), file3);

        MultipartBody.Part fileToUpload1 = MultipartBody.Part.createFormData("file1", file1.getName(), requestBody1);
        MultipartBody.Part fileToUpload2 = MultipartBody.Part.createFormData("file2", file2.getName(), requestBody2);
        MultipartBody.Part fileToUpload3 = MultipartBody.Part.createFormData("file3", file3.getName(), requestBody3);

        ApiConfig getResponse = AppConfig.getRetrofit().create(ApiConfig.class);
        Call<ServerResponse> call = getResponse.uploadMulFile(fileToUpload1, fileToUpload2,fileToUpload3);
        call.enqueue(new Callback<ServerResponse>() {
            @Override
            public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                ServerResponse serverResponse = response.body();
                if (serverResponse != null) {
                    if (serverResponse.getSuccess()) {
                        Toast.makeText(activity.getApplicationContext(), serverResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity.getApplicationContext(), serverResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    assert serverResponse != null;
                    Log.v("Response", serverResponse.toString());
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {

            }
        });
    }


    public void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            activity.startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(activity.getApplicationContext(), "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    public String getPath(Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = activity.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {

                e.printStackTrace();

            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        switch (requestCode) {
//            case FILE_SELECT_CODE:
//                if (resultCode == RESULT_OK) {
//                    // Get the Uri of the selected file
//                    Uri uri = data.getData();
//                    Log.d(TAG, "File Uri: " + uri.toString());
//                    // Get the path
//                    try {
//                        String path = getPath(uri);
//                        Log.d(TAG, "File Path: " + path);
//                    }catch (Exception e){
//                        e.printStackTrace();
//                    }
//
//
//                    // Get the file instance
//                    // File file = new File(path);
//                    // Initiate the upload
//                }
//                break;
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    //    public void drawfigure(){
//        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
//        dataset.addSeries(series);
//
//        // Now we create the renderer
//        XYSeriesRenderer renderer = new XYSeriesRenderer();
//        renderer.setLineWidth(2);
//        renderer.setColor(Color.RED);
//        // Include low and max value
//        renderer.setDisplayBoundingPoints(true);
//        // we add point markers
//        renderer.setPointStyle(PointStyle.CIRCLE);
//        renderer.setPointStrokeWidth(3);
//
//        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
//        mRenderer.addSeriesRenderer(renderer);
//
//
//        // We want to avoid black border
//        // transparent margins
//        mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
//        // Disable Pan on two axis
//        mRenderer.setPanEnabled(false, false);
//        mRenderer.setYAxisMax(35);
//        mRenderer.setYAxisMin(0);
//        mRenderer.setShowGrid(true); // we show the grid
//
//        GraphicalView chartView = ChartFactory.getLineChartView(MainActivity.this, dataset, mRenderer);
//
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
//        alertDialog.setTitle("Graph");
//        alertDialog.setMessage("Graph between Amplitude and sample freq");
//
//        //final EditText input = new EditText(MainActivity.this);
//        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.VERTICAL);
//
//        chartView.setLayoutParams(lp);
//
//        alertDialog.setView(chartView);
//
//        //input.setLayoutParams(lp);
//        //input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
//        //alertDialog.setView(input);
//        //alertDialog.setIcon(R.drawable.key);
//
//        alertDialog.setPositiveButton("OK",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        //record_file = input.getText().toString();
//                        //startRecord();
//                        dialog.dismiss();
//                    }
//                });
//
//        alertDialog.setNegativeButton("Cancel",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
//
//        alertDialog.show();
//    }

}
