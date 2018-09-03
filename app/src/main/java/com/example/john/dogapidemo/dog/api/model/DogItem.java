package com.example.john.dogapidemo.dog.api.model;

import android.support.annotation.NonNull;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

/**
 * A dummy item representing a piece of content.
 */
public class DogItem {
    /**
     * id is of form [this.title][suffix]
     * The suffix allows for multiple Dog Items with the same title
     */
    public final String id;
    /**
     * The breed of the dog is the title
     */
    private String title;
    private String url;
    private String randomUrl;
    private float rating = 2.5f;
    private List<String> subbreeds;

    public DogItem(String id, String title, String url) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.randomUrl = null;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof DogItem && id.equals(((DogItem) obj).id);
    }

    /**
     * Call from the main thread
     */
    public void clearRandomURlFromCache() {
        ImageLoader loader = ImageLoader.getInstance();

        if (randomUrl != null && !randomUrl.equals(url) && loader.isInited()) {
            loader.getMemoryCache().remove(randomUrl);
        }
        randomUrl = null;
    }

    /**
     *
     * @return randomUrl != null ? randomUrl : url
     */
    public String getCurrentURL() {
        return randomUrl != null ? randomUrl : url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRandomUrl() {
        return randomUrl;
    }

    public void setRandomUrl(String randomUrl) {
        this.randomUrl = randomUrl;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public List<String> getSubbreeds() {
        return subbreeds;
    }

    public void setSubbreeds(@NonNull List<String> subbreeds) {
        this.subbreeds = subbreeds;
    }
}
