package com.example.john.dogapidemo.dog.api;

import com.example.john.dogapidemo.dog.api.reponse.DogBreedResponseBody;
import com.example.john.dogapidemo.dog.api.reponse.DogBreedRandomImageResponseBody;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface DogApi {
    // Access the raw response
    @GET("breeds/list/all")
    Call<ResponseBody> getAllBreeds();

    @GET("breed/{breed}/images")
    Call<DogBreedResponseBody> getAllImagesForBreed(@Path("breed") String breed);

    @GET("breed/{breed}/images/random")
    Call<DogBreedRandomImageResponseBody> getRandomImageForBreed(@Path("breed") String breed);

    @GET("breed/{breed}/list")
    Call<DogBreedResponseBody> getAllSubbreedsForBreed(@Path("breed") String breed);

    @GET("breed/{breed}/{subbreed}/images/random")
    Call<DogBreedRandomImageResponseBody> getRandomImageForBreed(@Path("breed") String breed,
                                                 @Path("subbreed") String subbreed);
}
