package io.github.decodeit.smartbin;

import com.google.gson.annotations.SerializedName;

/**
 * Created by prince on 23/4/17.
 */

public class ServerResponse {

    // variable name should be same as in the json response from php
    @SerializedName("status")
    boolean status;
    @SerializedName("message")
    String message;

    String getMessage() {
        return message;
    }

    boolean getSuccess() {
        return status;
    }


}
