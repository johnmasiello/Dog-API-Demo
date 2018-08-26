package com.example.john.dogapidemo.dog.api;

/**
 * An interface to implement by any activity that uses DogContentFragment
 */
public interface DownloadCallback {

    void updateBreeds();

    void updateDogItem_URL(int position);

    void reportApiFailure(Throwable throwable);
}
