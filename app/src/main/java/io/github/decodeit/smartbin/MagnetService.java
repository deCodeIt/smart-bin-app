package io.github.decodeit.smartbin;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Rehmat on 4/10/2017.
 */

public class MagnetService implements SensorEventListener {

    private Activity activity;
    private SensorManager mSensorManager = null;
    private Sensor mSensor;
    private float[] magnet = new float[3];
    private boolean isReading = false;
    private TextView tv; // magnetic field reading TextView
    ArrayList<Float> magneticReading;
    private static final int MAX_NUM = 1000; // # of samples to collect before stopping
    private static final int SLIDING_WINDOW_SIZE = 10; // samples in sliding window to check for field change
    private static int num = 0;
    private boolean isReadingBaseMagneticFieldStrength = false; // set to true when reading base magnetic field for first time
    private float BASE_STRENGTH = 0.0f; // magnetic field in empty bin
    private float THRESHOLD = 5.0f; // currentStrength > BASE_STRENGTH + THRESHOLD
    private boolean metalFound = false;
    private float currentStrength=0, pastStrength=0, difference, SUM=0, maxInSample, minInSample; // useless but saves memory and speeds up

    MagnetService( Activity activity){
        this.activity = activity;
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        tv = (TextView) activity.findViewById(R.id.magnetic_field_tv);
        activity.findViewById(R.id.MF_start).setEnabled(false);
        isReadingBaseMagneticFieldStrength = true;
        start();

        //TODO disable changing of labels while calculating base magnetic field
    }

    public void register(){
        if(isReading) {
            // register the listener only if we are reading values otherwise save power
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    public void deRegister(){
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isReading) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    num++;
                    System.arraycopy(event.values, 0, magnet, 0, 3);
                    // do your updates like:

                    // get current magnetic field
                    currentStrength = getMagneticFieldStrength();

                    if(!metalFound && BASE_STRENGTH!=0 && currentStrength > THRESHOLD + BASE_STRENGTH){
                        // metal found
                        Log.d(MainActivity.MAGNET_TAG, "Metal Found");
                        // Set metal found label to true
                        ((Spinner)activity.findViewById(R.id.label_metal)).setSelection(1);
                    }
                    // append it in array
                    magneticReading.add(currentStrength);

                    // do processing
                    if (isReadingBaseMagneticFieldStrength) {
                        // collecting data for base magnetic field reading
                        updateTextField(currentStrength);
                        if (num < MAX_NUM) {
                            SUM += currentStrength;
                        } else {
                            BASE_STRENGTH = SUM / num; // set base magnetic field strength to the average of all samples
                            Log.d(MainActivity.MAGNET_TAG, "Base Magnetic Strength: " + BASE_STRENGTH);
                            stopCollectingSamplesForBaseStrength();
                        }
                    } else {
                        // applying sliding window technique
                        if(num < MAX_NUM) {
                            if (num > SLIDING_WINDOW_SIZE) {
                                // we have at least required number of reading to start/continue out sliding window
                                minInSample = Collections.min(magneticReading.subList(num - SLIDING_WINDOW_SIZE, num));
                                maxInSample = Collections.max(magneticReading.subList(num - SLIDING_WINDOW_SIZE, num));

                                difference = Math.abs(maxInSample - minInSample);
                                updateTextField(difference);
                            } else {
                                tv.setText("Reading...");
                            }
                        } else {
                            stopCollectingSamples();
                        }
                    }

                    break;
                default:
                    Log.d(MainActivity.MAGNET_TAG, "Unknown Sensor");
            }
        }
    }

    private void updateTextField(float strength){
        // updates the textView
        tv.setText(String.format(Locale.US, "%.2f", strength));
    }

    private float getMagneticFieldStrength(){
        // returns recorded current magnetic field strength
        return (float)Math.sqrt(magnet[0]*magnet[0] + magnet[1]*magnet[1] + magnet[2]*magnet[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(MainActivity.MAGNET_TAG, sensor.getName() + ":" + accuracy);
    }

    public void start(){
        // start Reading
        // initial values
        metalFound = false;
        SUM = 0;
        pastStrength = 0;
        num = 0;
        magneticReading = new ArrayList<>();
        isReading = true;
        ((Spinner)activity.findViewById(R.id.label_metal)).setSelection(0);

        //start reading
        register();
    }

    public void stopCollectingSamplesForBaseStrength() {
        // stop reading for base strength
        deRegister();
        isReadingBaseMagneticFieldStrength = false;
        isReading = false;
        magneticReading.clear(); // clear the array to free up memory
        activity.findViewById(R.id.MF_start).setEnabled(true); // enable the start button for next reading
    }

    public void stopCollectingSamples(){
        deRegister();
        isReading = false;
        String data = android.text.TextUtils.join(",",magneticReading);
        Log.d(MainActivity.MAGNET_TAG,"Samples:"+ magneticReading.size() + ":" +data);
        MainActivity.db.insertMagnetData(magneticReading);
        magneticReading.clear(); // clear the array to free up memory
        activity.findViewById(R.id.MF_start).setEnabled(true); // enable the start button for next reading
    }
}
