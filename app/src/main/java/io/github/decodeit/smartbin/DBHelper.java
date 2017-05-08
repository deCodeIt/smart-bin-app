package io.github.decodeit.smartbin;

import android.app.Activity;
import android.content.ContentValues;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Spinner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by laststnd on 13/4/17.
 */

public class DBHelper extends SQLiteOpenHelper {
    // stores the data collected in sqlite tables
    private Activity activity;
    private DateFormat df = new SimpleDateFormat("yy_MM_dd_HH_mm_ss", Locale.US);
    Date dateObj;

    private static final String DB_TAG = "DB";

    private static final String DATABASE_NAME = "smart_bin_db.db";
    private static final String APP_STORAGE_DIR = "SmartBin";
    private static final String WIFI_TABLE_NAME = "wifi_data";
    private static final String SOUND_TABLE_NAME = "sound_data";
    private static final String MAGNET_TABLE_NAME = "magnet_data";

    // wifi table columns
    private static final String SIGNAL_WIFI_COLUMN_NAME = "signals"; // csv (comma separated values)
    private static final String LABEL_WIFI_COLUMN_NAME = "label"; // csv

    // sound table columns
    // TODO your column names

    // magnet table columns
    private static final String SIGNAL_MAGNET_COLUMN_NAME = "field_strength"; // csv (comma separated values)
    private static final String LABEL_MAGNET_COLUMN_NAME = "label"; // csv


    public DBHelper(Activity activity){
        super(activity.getApplicationContext(),DATABASE_NAME,null,4);
        Log.d(DB_TAG,"IN DBHelper");
        this.activity = activity;

        // initialize sound storage directory
        File f = new File(getAppStorageDirectory(), SOUND_TABLE_NAME);
        if (!f.exists()) {
            f.mkdirs();
        }

        f = new File(getAppStorageDirectory(), WIFI_TABLE_NAME);
        if (!f.exists()) {
            f.mkdirs();
        }

        f = new File(getAppStorageDirectory(), MAGNET_TABLE_NAME);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    public String getAppStorageDirectory() {
        return Environment.getExternalStorageDirectory() + File.separator + APP_STORAGE_DIR;
    }

    public String getSoundStorageDir(){
        return getAppStorageDirectory() + File.separator + SOUND_TABLE_NAME;
    }

    public String getWifiStorageDir(){
        return getAppStorageDirectory() + File.separator + WIFI_TABLE_NAME;
    }

    public String getMagnetStorageDir(){
        return getAppStorageDirectory() + File.separator + MAGNET_TABLE_NAME;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(DB_TAG,"IN OnCreate");
        db.execSQL(
                "create table " + WIFI_TABLE_NAME  +
                        " (id integer primary key autoincrement, "+SIGNAL_WIFI_COLUMN_NAME+" text," + LABEL_WIFI_COLUMN_NAME + " text)"
        );

        db.execSQL(
                "create table " + MAGNET_TABLE_NAME  +
                        " (id integer primary key autoincrement, "+SIGNAL_MAGNET_COLUMN_NAME+" text," + LABEL_WIFI_COLUMN_NAME + " text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DB_TAG,"IN onUpgrade");
        db.execSQL("DROP TABLE IF EXISTS "+WIFI_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+MAGNET_TABLE_NAME);
        onCreate(db);
    }

    public boolean insertWifiData(ArrayList<Integer> signal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SIGNAL_WIFI_COLUMN_NAME,android.text.TextUtils.join(",",signal));
        cv.put(LABEL_WIFI_COLUMN_NAME, getLabel());
        db.insert(WIFI_TABLE_NAME,null,cv);
        db.close();

        // save to file
        return saveWifiDataToFile(signal);
    }

    private boolean saveWifiDataToFile(ArrayList<Integer> signal) {
        // saves the wifi Reading by appending it to a file
        boolean ok=false;

        try {
            String filename = "wifi_data.txt";
            String FULL_FILE_PATH = MainActivity.db.getWifiStorageDir() + File.separator + filename;
            Log.d(MainActivity.WIFI_TAG, FULL_FILE_PATH);
            File path = new File(FULL_FILE_PATH);
//            FileOutputStream outFile = new FileOutputStream(path, true);
            FileOutputStream outFile = new FileOutputStream(path);
            String data = android.text.TextUtils.join(",",signal) + "," + getLabel();
            for(int i=0; i<data.length(); ++i) {
                outFile.write(data.charAt(i));
            }
            outFile.write('\n'); // a new line character
            outFile.close();
            ok=true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            ok=false;
        } catch (IOException e) {
            ok=false;
            e.printStackTrace();
        }
        return ok;
    }

    public boolean insertMagnetData(ArrayList<Float> signal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SIGNAL_MAGNET_COLUMN_NAME,android.text.TextUtils.join(",",signal));
        cv.put(LABEL_MAGNET_COLUMN_NAME, getLabel());
        db.insert(MAGNET_TABLE_NAME,null,cv);
        db.close();

        // save to file
        return saveMagnetDataToFile(signal);
    }

    private boolean saveMagnetDataToFile(ArrayList<Float> signal) {
        // saves the magnet Reading by appending it to a file
        boolean ok=false;

        try {
            String filename = "magnet_data.txt";
            String FULL_FILE_PATH = MainActivity.db.getMagnetStorageDir() + File.separator + filename;
            Log.d(MainActivity.MAGNET_TAG, FULL_FILE_PATH);
            File path = new File(FULL_FILE_PATH);
            FileOutputStream outFile = new FileOutputStream(path, true);
            String data = android.text.TextUtils.join(",",signal) + "," + getLabel();
            for(int i=0; i<data.length(); ++i) {
                outFile.write(data.charAt(i));
            }
            outFile.write('\n'); // a new line character
            outFile.close();
            ok=true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            ok=false;
        } catch (IOException e) {
            ok=false;
            e.printStackTrace();
        }
        return ok;
    }


    public ArrayList<String> getWifiData(){
        ArrayList<String> signalData;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM "+WIFI_TABLE_NAME,null);
        res.moveToFirst();
        signalData = new ArrayList<String>(res.getCount());
        while(!res.isAfterLast()){
            signalData.set(res.getPosition(),res.getString(res.getColumnIndex(SIGNAL_WIFI_COLUMN_NAME)) + "," + res.getString(res.getColumnIndex(LABEL_WIFI_COLUMN_NAME)) );
            res.moveToNext();
        }
        return signalData;
    }

    public ArrayList<String> getMagnetData(){
        ArrayList<String> signalData;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM "+MAGNET_TABLE_NAME,null);
        res.moveToFirst();
        signalData = new ArrayList<String>(res.getCount());
        while(!res.isAfterLast()){
            signalData.set(res.getPosition(),res.getString(res.getColumnIndex(SIGNAL_MAGNET_COLUMN_NAME)) + "," + res.getString(res.getColumnIndex(LABEL_MAGNET_COLUMN_NAME)) );
            res.moveToNext();
        }
        return signalData;
    }

    private String getLabel(){
        // gets the current label set in MainActivity by user for current recorded data
        String label = String.valueOf(((Spinner)activity.findViewById(R.id.label_paper_clothes)).getSelectedItem()) + "," +
                String.valueOf(((Spinner)activity.findViewById(R.id.label_water)).getSelectedItem()) + ",0";

        return label;

    }

    public String getSoundLabel(){
        // gets the current label set in MainActivity by user for current recorded data
        dateObj = new Date();
        String label = "P-"+String.valueOf(((Spinner)activity.findViewById(R.id.label_paper_clothes)).getSelectedItem()) + "_W-" +
                String.valueOf(((Spinner)activity.findViewById(R.id.label_water)).getSelectedItem()) + "_M-0" +
                "_"+ df.format(dateObj) + ".wav";

        return label;
    }
}
