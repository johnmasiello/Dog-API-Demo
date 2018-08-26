package com.example.john.dogapidemo.dog.api.reponse;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DogBreedResponseBody {

    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("message")
    @Expose
    private List<String> urls;


    public String getStatus() {
        return status;
    }

    public List<String> getUrls() {
        return urls;
    }
}
