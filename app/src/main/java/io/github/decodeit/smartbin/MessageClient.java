package io.github.decodeit.smartbin;

import android.app.Activity;
import android.util.Log;

import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by laststnd on 11/4/17.
 */

public class MessageClient {
    // send the message to server about the playing time of the file and its duration
    private static final int port = 5555;

    private Activity activity;
    private Socket s = null;
    private String hostIp = null;
    private Message message = null;

    public MessageClient(Activity activity, String hostIp) {
        this.activity = activity;
        this.hostIp = hostIp;
    }

    public void createMessage(long startTime, long duration, boolean terminate) {
        message = new Message(startTime, duration, terminate);
    }

    public void setUp(){
        try{
            s = new Socket(hostIp,port);
            try {
                ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
                if(message != null) {
                    os.writeObject(message);
                    s.close();
                } else {
                    Log.d(MainActivity.WIFI_TAG, "Empty Message");
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
