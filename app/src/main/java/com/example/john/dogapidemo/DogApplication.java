package com.example.john.dogapidemo;

import android.app.Application;
import android.content.ComponentCallbacks2;

import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * Created by john on 3/11/18.
 * Custom Application to clear the underlying LRUCache when trim memory is called
 */

public class DogApplication extends Application implements ComponentCallbacks2 {

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
