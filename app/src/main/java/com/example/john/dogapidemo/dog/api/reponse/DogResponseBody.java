package com.example.john.dogapidemo.dog.api.reponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DogResponseBody {
    private final String status;
    private final List<String> breeds;

    public DogResponseBody(String json) {
        breeds = new ArrayList<>();
        String status;
        JSONObject oJson;
        try {
            oJson = new JSONObject(json);
            status = oJson.getString("status");
            JSONObject breeds = oJson.getJSONObject("message");
            JSONArray arrJson = breeds.names();
            for (int i = 0; i < arrJson.length(); i++) {
                this.breeds.add(arrJson.getString(i));
            }

        } catch (JSONException ignore) {
            status = null;
        }
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getBreeds() {
        return breeds;
    }
}
