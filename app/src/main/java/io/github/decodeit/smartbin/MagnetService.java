package io.github.decodeit.smartbin;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

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
    private float currentStrength; // useless but saves memory and speeds up

    MagnetService( Activity activity){
        this.activity = activity;
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        tv = (TextView) activity.findViewById(R.id.magnetic_field_tv);
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
                    System.arraycopy(event.values, 0, magnet, 0, 3);
                    // do your updates like:

                    // get current magnetic field
                    currentStrength = getMagneticFieldStrength();

                    // update the textview
                    updateTextField(currentStrength);

                    // append it in array
                    magneticReading.add(currentStrength);

                    break;
                default:
                    Log.d(MainActivity.MAGNET_TAG, "Unknown Sensor");
            }
        }
    }

    private void updateTextField(float strength){
        // updates the textView
        tv.setText(String.valueOf(strength));
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
        magneticReading = new ArrayList<>();
        isReading = true;
        register();
    }

    public void stop(){
        isReading = false;
        deRegister();
        // save/process the collected values
        ArrayList<Float> recorderMagneticReadings = magneticReading;
        magneticReading = new ArrayList<>();
        String data = android.text.TextUtils.join(",",recorderMagneticReadings);
        Log.d(MainActivity.MAGNET_TAG,"Samples:"+ recorderMagneticReadings.size() + ":" +data);
        recorderMagneticReadings.clear(); // clear the array to free up memory

        // data is the thing to be stored, overall magnetic field values collected over time
    }
}
