package com.example.john.dogapidemo.dog.api;

import java.util.List;

/**
 * An interface to connect the Dog Repository to the ui
 */
public interface DetailDownloadCallback {
    void updateDogWithRandomImage();

    void onFailureFetchDogRandomImage();

    void updateSubBreedList(List<String> subBreeds);

    void onFailureFetchSubbreeds();
}
