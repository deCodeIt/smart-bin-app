package io.github.decodeit.smartbin;

import android.app.Activity;
import android.widget.Toast;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by laststnd on 11/4/17.
 */

public class MessageServer {
    // server will read the time at which to play the file as well as the duration from client
    private Activity activity;
    private static final int port = 5555;
    private ServerSocket ss = null;
    private Socket skt;

    MessageServer(Activity activity) {
        this.activity = activity;
    }

    public void setUp(){
        // setup the server
        try{
            ss = new ServerSocket(port);
            boolean flag = false; // set to true => time to exit
            while(!flag) {
                skt = ss.accept();

                try {
//                ObjectOutputStream os = new ObjectOutputStream(skt.getOutputStream());
                    ObjectInputStream oi = new ObjectInputStream(skt.getInputStream());
                    try {
                        final Message m = (Message) oi.readObject();
//                    m.printMessage();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, "Begin at: " + m.getStartTime() + ", duration: " + m.getDuration(), Toast.LENGTH_SHORT).show();
                                MainActivity.sH.setDelayedPlayingService(m); // set up playing media after the duration
                            }
                        });
                        flag = m.isLast(); // check if this is the last message
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                skt.close();
            }
            ss.close();
        } catch (java.io.IOException e){
            e.printStackTrace();
        }
    }
}
