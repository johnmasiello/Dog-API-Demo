package com.example.john.dogapidemo.dog.api;

import android.util.Log;

import com.example.john.dogapidemo.dog.api.model.DogItem;
import com.example.john.dogapidemo.dog.api.reponse.DogBreedResponseBody;
import com.example.john.dogapidemo.dog.api.reponse.DogResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DogRepository {

    private final DogApi dogApi;
    private final List<DownloadCallback> downloadCallbacks = new ArrayList<>(1);
    private final List<DetailDownloadCallback> detailDownloadCallbacks = new ArrayList<>(1);

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, DogItem> ITEM_MAP = new HashMap<>();


    /**
     * An array of sample (dummy) items. Items must be added synchronously
     */
    public static final List<DogItem> ITEMS = new ArrayList<>();

    public DogRepository(DogApi dogApi) {
        this.dogApi = dogApi;
        loadBreeds();
    }

    public void registerDownloadCallback(DownloadCallback callback) {
        downloadCallbacks.add(callback);
        callback.updateBreeds();
    }

    public void unregisterDownloadCallback(DownloadCallback callback) {
        downloadCallbacks.remove(callback);
    }

    private void loadBreeds() {
        dogApi.getAllBreeds().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String strResponseBody = "body";
                try {
                    strResponseBody = response.body().string();
                    response.body().close();

                } catch (IOException e) {
                    Log.e("Repo", e.getLocalizedMessage());
                }
                // Use the raw response to set the DogResponseBody
                DogResponseBody dogResponseBody = new DogResponseBody(strResponseBody);
                List<String> breeds = dogResponseBody.getBreeds();
                for (String breed : breeds) {
                    addDogItemIfDoesNotExist(breed);
                    Log.d("Repo", breed);
                }
                for (DownloadCallback callback : downloadCallbacks)
                    callback.updateBreeds();

                Log.d("Repo", strResponseBody);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                for (DownloadCallback callback : downloadCallbacks)
                    callback.reportApiFailure(t);

                Log.e("Repo", "Unable to load breeds");
            }
        });
    }

    /**
     * <p>Wraps storing the item in both the hash map and the array list, the latter of which backs the array adapter</p>
     * <p>Continues updating the item using DOG API, if the item is not fully initialized, ie the thumbnail url</p>
     * @param title Title or breed of the dog
     */
    private void addDogItemIfDoesNotExist(String title) {

        DogItem dog;

        if ( !DogRepository.ITEM_MAP.containsKey(title) ) {

            dog = createDogItem(title);
            DogRepository.ITEM_MAP.put(dog.id, dog);
            DogRepository.ITEMS.add(dog);

        } else {
            dog = DogRepository.ITEM_MAP.get(title);
        }

        // Update item using DOG API
        if (dog.getUrl() == null) {
            loadBreedImagesUrl(dog);
        }
    }

    private DogItem createDogItem(String title) {
        String properTitle = toTitleCase(title);

        return new DogItem(title, properTitle, null);
    }

    private String toTitleCase(String word) {
        return String.valueOf(Character.toTitleCase(word.charAt(0))) +
                word.substring(1);
    }

    private void loadBreedImagesUrl(DogItem dog) {
        dogApi.getAllImagesForBreed(dog.getTitle().toLowerCase()).enqueue(new Callback<DogBreedResponseBody>() {
            @Override
            public void onResponse(Call<DogBreedResponseBody> call, Response<DogBreedResponseBody> response) {
                DogBreedResponseBody dogResponse = response.body();

                if (dogResponse != null) {
                    String breed = parseBreedFromUrl(call.request().url());

                    // Fetch the dog item
                    DogItem dogItem = ITEM_MAP.get(breed);

                    if (dogItem != null) {
                        dogItem.setUrl(dogResponse.getUrls().get(0));

                        int position = findPosition(dogItem);

                        if (position >= 0) {
                            for (DownloadCallback callback : downloadCallbacks)
                                callback.updateDogItem_URL(position);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<DogBreedResponseBody> call, Throwable t) {
                // NO OP
            }
        });
    }

    private int findPosition(DogItem dogItem) {
        return ITEMS.indexOf(dogItem);
    }

    private String parseBreedFromUrl(HttpUrl url) {
        if (url == null)
            return "";

        List<String> segments = url.pathSegments();
        for (int i = 0; i < segments.size(); i++) {
            if ("breed".equals(segments.get(i)))
                return segments.get(i + 1);
        }
        return "";
    }
}
