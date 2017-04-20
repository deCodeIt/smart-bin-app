package io.github.decodeit.smartbin;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.Serializable;

/**
 * Created by laststnd on 11/4/17.
 */

public class Message implements Serializable {
    private static final long serialVersionUID = 123123123123123L;

    private String fileName; // file to be played
    private long startTime; // time to start recording/playing the sound
    private long duration; // duration of clip in seconds
    private boolean isLastMessage; // set to true if this is the last message

    public Message(String fileName, long startTime, long duration, boolean isLastMessage) {
        this.fileName = fileName;
        this.startTime = startTime;
        this.duration = duration;
        this.isLastMessage = isLastMessage;
    }

    public Message(long startTime, long duration, boolean isLastMessage) {
        this.fileName = "sine_wave.wav";
        this.startTime = startTime;
        this.duration = duration;
        this.isLastMessage = isLastMessage;
    }

    public String getFileName() { return fileName; }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isLast() {
        return isLastMessage;
    }

    public void printMessage() {
        Log.d(MainActivity.WIFI_TAG,"Begin at: " + getStartTime() + ", duration: " + getDuration());
    }
}
