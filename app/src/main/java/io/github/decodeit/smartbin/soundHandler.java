package io.github.decodeit.smartbin;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by prince on 7/4/17.
 */

public class soundHandler {

    public static MediaPlayer mediaPlayer;
    public static MediaRecorder recorder;
    //String record_file = "";
    public static File audiofile = null;

    public static void audioPlay(String path, String file, Context context){
        Uri myUri = Uri.parse(path + File.separator + file); // initialize Uri here
        mediaPlayer= new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(context, myUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.e("Starting", "Media Player");
        } catch (Exception e) {
            e.printStackTrace();
        }

        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    public static MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

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

    public static void startRecord(String record_file){
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

    private static void stop_record(MediaRecorder mr){
        mr.stop();
        mr.reset();   // You can reuse the object by going back to setAudioSource() step
        Log.e("Stopping", "Recording");
        mr.release();
    }

}
