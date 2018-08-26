package com.example.john.dogapidemo.dog.api.reponse;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class DogResponseBody {
    @SerializedName("status")
    @Expose
    private final String status;

    @SerializedName("message")
    @Expose
    private final List<Data> message;

    public DogResponseBody(String status, List<Data> message) {
        this.status = status;
        this.message = message;
    }

    /**
     *  @return a List of the breed names, which are the fields in the response body
     *  to the DogApi
     */
    public List<String> fetchBreeds() {
        List<String> breeds = new ArrayList<>();
        if (message != null) {
            for (Data datum : message) {
                breeds.add(datum.getKey());
            }
        }
        return breeds;
    }

    public String getStatus() {
        return status;
    }
}
