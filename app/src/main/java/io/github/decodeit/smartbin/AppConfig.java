package io.github.decodeit.smartbin;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by prince on 23/4/17.
 */

public class AppConfig {
    ///  Base url to communicate
    private static String BASE_URL = "http://10.20.0.108/upload.php/";

    static Retrofit getRetrofit() {

        return new Retrofit.Builder()
                .baseUrl(AppConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
