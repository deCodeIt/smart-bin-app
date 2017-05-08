package io.github.decodeit.smartbin;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by prince on 23/4/17.
 */

public class AppConfig {
    ///  Base url to communicate
    private static String BASE_URL = "http://192.168.150.1/classifier/predictor.php/";
    //private static String BASE_URL = "http://10.20.0.99/upload_img.php/";

    static Retrofit getRetrofit() {

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(180, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .build();
        return new Retrofit.Builder()
                .baseUrl(AppConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
