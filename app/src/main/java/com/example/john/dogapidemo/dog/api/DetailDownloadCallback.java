package com.example.john.dogapidemo.dog.api;

/**
 * An interface to implement by any Detail fragment that uses DogContentFragment
 */
public interface DetailDownloadCallback {
    void updateDogWithRandomImage();

    void updateSubBreedList(String[] subBreeds);
}
