package io.github.decodeit.smartbin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaActionSound;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;
import android.widget.LinearLayout;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by prince on 7/4/17.
 */

public class soundHandler {

    private Activity activity;
    public  MediaPlayer mediaPlayer;
    public  MediaRecorder recorder;
    //String record_file = "";
    public  File audiofile = null;
    public final int sampleRate = 8000;
    public AudioRecord audio;
    public int bufferSize;
    public double lastLevel = 0;
    public Thread thread;
    public final int SAMPLE_DELAY = 1;  // in ms
    public final int DELAY = 5000; // time in millisecond after which have to stop recording
    public ArrayList<Short> amps = new ArrayList<Short>();
    private int loopVar, bufferReadResult;
    private short[] buffer;
    private  ExecutorService threadPool;
    //int xval = 0;
    //XYSeries series;


    // constructor
    soundHandler(Activity act){
        this.activity = act;
    }

    public void audioPlay(String path, String file){
        Uri myUri = Uri.parse(path + File.separator + file); // initialize Uri here
        mediaPlayer= new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(activity.getBaseContext(), myUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.e("Starting", "Media Player");
        } catch (Exception e) {
            e.printStackTrace();
        }

        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    public MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            // TODO Auto-generated method stub
            mp.stop();
            mp.release();
            Log.e("Stopping", "Media Player");
            mp = null;
        }
    };

    public void playerStop(MediaPlayer mp){
        mp.stop();
        mp.release();
        Log.e("Force Stopping", "Media Player");
        mp = null;
    }

    public void startRecord(String record_file){
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        // creating file
        //File dir = Environment.getExternalStorageDirectory();
        File dir = Environment.getExternalStorageDirectory();
        try {
            audiofile = File.createTempFile(record_file, ".3gp", dir);
            //File au = File
        } catch (IOException e) {
            Log.e("Error", "external storage access error");
            return;
        }
        try {

            recorder.setOutputFile(audiofile.getAbsolutePath());
            //recorder.setMaxDuration(40000);
            recorder.prepare();
            recorder.start();   // Recording is now started
            Log.e("Starting", "Recording");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stop_record(MediaRecorder mr){
        mr.stop();
        mr.reset();   // You can reuse the object by going back to setAudioSource() step
        Log.e("Stopping", "Recording");
        mr.release();
    }

    public void initialiseAudioRecorder(){
        //sampleRate = 8000;
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);

//            buffer = new short[bufferSize];
            Log.d(MainActivity.SOUND_TAG,"InInitializeAudioRecorder, bufferSize: "+bufferSize);

            final Runnable audioReadRunnable = new Runnable() {
                @Override
                public void run() {
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
                    try {
                        threadPool.execute(audioReadRunnable);
                    } catch (java.util.concurrent.RejectedExecutionException e){
                        Log.d(MainActivity.SOUND_TAG,"Thread Terminated");
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
//                    threadPool.shutdown();
                }
            });

            Log.e("Initialize", "audioRecord Initialized");

            Log.e("Starting","Recorder");
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
                for(loopVar=0; loopVar<bufferReadResult; loopVar++) {
                    amps.add(buffer[loopVar]);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAudioRecord(){
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
