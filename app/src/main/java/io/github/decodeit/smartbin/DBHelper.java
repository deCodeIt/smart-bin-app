package io.github.decodeit.smartbin;

import android.app.Activity;
import android.content.ContentValues;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Spinner;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by laststnd on 13/4/17.
 */

public class DBHelper extends SQLiteOpenHelper {
    // stores the data collected in sqlite tables
    private Activity activity;

    private static final String DB_TAG = "DB";

    private static final String DATABASE_NAME = "smart_bin_db.db";
    private static final String WIFI_TABLE_NAME = "wifi_data";
    private static final String SOUND_TABLE_NAME = "sound_data";
    private static final String MAGNET_TABLE_NAME = "magnet_data";

    // wifi table columns
    private static final String SIGNAL_WIFI_COLUMN_NAME = "signals"; // csv (comma separated values)
    private static final String LABEL_WIFI_COLUMN_NAME = "label"; // csv

    // sound table columns
    // TODO your column names

    // magnet table columns
    // TODO your column names


    public DBHelper(Activity activity){
        super(activity.getApplicationContext(),DATABASE_NAME,null,2);
        Log.d(DB_TAG,"IN DBHelper");
        this.activity = activity;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(DB_TAG,"IN OnCreate");
        db.execSQL(
                "create table " + WIFI_TABLE_NAME  +
                        " (id integer primary key autoincrement, "+SIGNAL_WIFI_COLUMN_NAME+" text," + LABEL_WIFI_COLUMN_NAME + " text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DB_TAG,"IN onUpgrade");
        db.execSQL("DROP TABLE IF EXISTS "+WIFI_TABLE_NAME);
        onCreate(db);
    }

    public boolean insertWifiData(ArrayList<Integer> signal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SIGNAL_WIFI_COLUMN_NAME,android.text.TextUtils.join(",",signal));
        cv.put(LABEL_WIFI_COLUMN_NAME, getLabel());
        db.insert(WIFI_TABLE_NAME,null,cv);
        db.close();
        return true;
    }

    // TODO Insert function and query for Magnet Data @ Rehmat


    // TODO Insert function and query for Sound Data @ Pradeep

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

    private String getLabel(){
        // gets the current label set in MainActivity by user for current recorded data
        String label = String.valueOf(((Spinner)activity.findViewById(R.id.label_paper_clothes)).getSelectedItem()) + "," +
                String.valueOf(((Spinner)activity.findViewById(R.id.label_water)).getSelectedItem()) + "," +
                String.valueOf(((Spinner)activity.findViewById(R.id.label_metal)).getSelectedItem());

        return label;
    }
}
