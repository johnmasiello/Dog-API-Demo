package com.example.john.dogapidemo.dog.api.reponse;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DogBreedRandomImageResponseBody {
    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("message")
    @Expose
    private String url;

    public String getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }
}
