package com.example.john.dogapidemo.dog.api;

/**
 * * An interface to connect the Dog Repository to the ui
 */
public interface DownloadCallback {

    void updateBreeds();

    void updateDogItem_URL(int position);

    void reportApiFailure(Throwable throwable);
}
