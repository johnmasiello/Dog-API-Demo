package com.example.john.dogapidemo.core;

import android.app.Application;
import android.content.ComponentCallbacks2;

import com.example.john.dogapidemo.BuildConfig;
import com.example.john.dogapidemo.dog.api.DogApi;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by john on 3/11/18.
 * Custom Application to clear the underlying LRUCache when trim memory is called
 */

public class DogApplication extends Application implements ComponentCallbacks2 {
    private static final int GLOBAL_TIMEOUT = 30; // seconds
    private final HttpUrl endpoint = HttpUrl.parse("https://dog.ceo/api/");

    private DogApi dogApi;

    public DogApi fetchDogApi() {
        if (dogApi == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(GLOBAL_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(GLOBAL_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(GLOBAL_TIMEOUT, TimeUnit.SECONDS)
                    .build();

            Gson gson = new Gson();

            dogApi = new Retrofit.Builder()
                    .client(client)
                    .baseUrl(endpoint)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .validateEagerly(BuildConfig.DEBUG)
                    .build()
                    .create(DogApi.class);
        }
        return dogApi;
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            ImageLoader imageLoader = ImageLoader.getInstance();
            if (imageLoader.isInited()) {
                imageLoader.getMemoryCache().clear();
            }
        }
        super.onTrimMemory(level);
    }
}