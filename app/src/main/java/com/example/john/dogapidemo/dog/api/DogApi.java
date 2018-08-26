package com.example.john.dogapidemo.dog.api;

import com.example.john.dogapidemo.dog.api.reponse.DogBreedResponseBody;
import com.example.john.dogapidemo.dog.api.reponse.DogBreedRandomImageResponseBody;
import com.example.john.dogapidemo.dog.api.reponse.DogResponseBody;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface DogApi {
    @GET("/breeds/list/all")
    Call<DogResponseBody> getAllBreeds();
    // TODO the component making the api call should pull the fields as values into the data model
    // for instance, I do not care of the value for the field named 'sheepdog'; I want only the field name

    @GET("/breed/{breed}/images")
    Call<DogBreedResponseBody> getAllImagesForBreed(@Path("breed") String breed);

    @GET("/breed/{breed}/images/random")
    Call<DogBreedRandomImageResponseBody> getRandomImageForBreed(@Path("breed") String breed);

    @GET("/breed/{breed}/list")
    Call<DogBreedResponseBody> getAllSubbreedsForBreed(@Path("breed") String breed);

    @GET("/breed/{breed}/{subbreed}/images/random")
    Call<DogBreedRandomImageResponseBody> getRandomImageForBreed(@Path("breed") String breed,
                                                 @Path("subbreed") String subbreed);
}
