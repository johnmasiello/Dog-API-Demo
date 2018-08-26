package com.example.john.dogapidemo.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.example.john.dogapidemo.dog.api.DetailDownloadCallback;
import com.example.john.dogapidemo.dog.api.model.DogItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

// TODO finish moving the functionality of this class to DogApi calls using Retrofit 2
/**
 * A headless fragment that loads the content for DOG API. It is reusable for each request.
 *
 */
public class DogContentFragment extends Fragment {

    private static final String FRAGMENT_KEY = "DogContentFragment";
    private static final String[] URLS = {
            "https://dog.ceo/api/breeds/list/all",
            "https://dog.ceo/api/breed/%s/images",
            "https://dog.ceo/api/breed/%s/images/random",
            "https://dog.ceo/api/breed/%s/list",
            "https://dog.ceo/api/breed/%s/%s/images/random"
        };

    private static final int BREEDS = 0;
    private static final int BREED_ALL_IMAGES = 1;
    private static final int BREED_RANDOM_IMAGE = 2;
    private static final int BREED_LIST_SUB_BREEDS = 3;
    private static final int BREED_SUB_RANDOM_IMAGE = 4;

    private WeakReference<DetailDownloadCallback> detailDownloadCallback;
    private ArrayList<AsyncTask> asyncTasks;

    public DogContentFragment() {
        asyncTasks = new ArrayList<>(100);
    }

    void loadBreedRandomImageUrl(DogItem dog, @NonNull WeakReference<DetailDownloadCallback>
            detailDownloadCallback) {

        this.detailDownloadCallback = detailDownloadCallback;
        asyncTasks.add(new RequestDogTask(new WeakReference<>(this)).execute(
                new DogRequestItem(dog, BREED_RANDOM_IMAGE)
        ));
    }

    void loadSubBreedList(DogItem dog, @NonNull WeakReference<DetailDownloadCallback>
            detailDownloadCallback) {

        this.detailDownloadCallback = detailDownloadCallback;
        asyncTasks.add(new RequestDogTask(new WeakReference<>(this)).execute(
                new DogRequestItem(dog, BREED_LIST_SUB_BREEDS)
        ));
    }

    void loadSubBreedRandomImageUrl(DogItem dog, String subbreed,
            @NonNull WeakReference<DetailDownloadCallback> detailDownloadCallback) {

        this.detailDownloadCallback = detailDownloadCallback;
        asyncTasks.add(new RequestDogTask(new WeakReference<>(this)).execute(
                new DogRequestItem(dog, BREED_SUB_RANDOM_IMAGE, subbreed)
        ));
    }

    private void handleResponse(DogRequestItem responseItem, Object result) {

        switch (responseItem.requestIndex) {
            case BREEDS:
            case BREED_ALL_IMAGES:
                break;

            case BREED_RANDOM_IMAGE:
            case BREED_SUB_RANDOM_IMAGE:
                if (detailDownloadCallback != null) {
                    DogItem dog = responseItem.dogItem;

                    assert dog != null;
                    dog.clearRandomURlFromCache();
                    dog.setRandomUrl((String) result);

                    // WeakReference, to check if callback is sent to GC
                    DetailDownloadCallback callback = detailDownloadCallback.get();

                    if (callback != null) {
                        callback.updateDogWithRandomImage();
                        return;
                    }
                }
                break;

            case BREED_LIST_SUB_BREEDS:
                if (detailDownloadCallback != null) {
                    DogItem dog = responseItem.dogItem;

                    assert dog != null;

                    // WeakReference, to check if callback is sent to GC
                    DetailDownloadCallback callback = detailDownloadCallback.get();

                    if (callback != null) {
                        callback.updateSubBreedList(((String[]) result));
                        return;
                    }
                }
                break;

            default:
                return;
        }
        Log.d("DownloadTask", "download callback is unset; unable to handle response");
    }

    private String makeURL(DogRequestItem dogRequestItem) {
        int index = dogRequestItem.requestIndex;
        switch (index) {
            case BREED_ALL_IMAGES:
            case BREED_RANDOM_IMAGE:
            case BREED_LIST_SUB_BREEDS:
                assert dogRequestItem.dogItem != null;
                return String.format(URLS[index], dogRequestItem.dogItem.getTitle().toLowerCase());

            case BREED_SUB_RANDOM_IMAGE:
                assert  dogRequestItem.dogItem != null;
                assert dogRequestItem.aux != null;
                return String.format(URLS[index], dogRequestItem.dogItem.getTitle().toLowerCase(),
                        dogRequestItem.aux);

            default:
                return URLS[index];
        }
    }

    static DogContentFragment getInstance(FragmentManager fragmentManager) {
        DogContentFragment fragment = (DogContentFragment) fragmentManager.findFragmentByTag(FRAGMENT_KEY);

        if (fragment == null) {
            fragment = new DogContentFragment();
            fragmentManager.beginTransaction().add(fragment, FRAGMENT_KEY).commit();
        }

        return fragment;
    }

    @Override
    public void onDestroy() {
        if (asyncTasks != null) {
            for (AsyncTask task : asyncTasks) {
                task.cancel(true);
            }
        }
        super.onDestroy();
    }

    public static String makeTitleCase(String word) {
        return String.valueOf(Character.toTitleCase(word.charAt(0))) +
                word.substring(1);
    }

    static class DogRequestItem {
        final DogItem dogItem;
        final int requestIndex;
        final Object aux;

        DogRequestItem(DogItem dogItem, int requestIndex) {
            this.dogItem = dogItem;
            this.requestIndex = requestIndex;
            this.aux = null;
        }

        DogRequestItem(@Nullable DogItem dogItem, int requestIndex, Object aux) {
            this.dogItem = dogItem;
            this.requestIndex = requestIndex;
            this.aux = aux;
        }
    }

    public static class RequestDogTask extends AsyncTask<DogRequestItem, Integer, Object> {

        private DogRequestItem dogRequestItem;
        private WeakReference<DogContentFragment> dogContentFragmentWeakReference;

        RequestDogTask(WeakReference<DogContentFragment> dogContentFragmentWeakReference) {
            this.dogContentFragmentWeakReference = dogContentFragmentWeakReference;
        }

        @Override
        protected Object doInBackground(DogRequestItem... dogRequestItems) {

            if (!isCancelled()) {
                dogRequestItem = dogRequestItems[0];

                HttpURLConnection connection = null;

                try {
                    URL url;
                    DogContentFragment fragment = dogContentFragmentWeakReference.get();

                    if (fragment != null) {
                        url = new URL(fragment.makeURL(dogRequestItem));

                    } else {
                        throw new IOException("Parent fragment is dead");
                    }

                    connection = ((HttpURLConnection) url.openConnection());
                    // For this use case, set HTTP method to GET.
                    connection.setRequestMethod("GET");
                    // Already true by default but setting just in case; needs to be true since this request
                    // is carrying an input (response) body.
                    connection.setDoInput(true);

                    connection.connect();

                } catch (IOException e) {
                    Log.e("DownloadTask", "Unable to open connection: "+e.getMessage());

                    if (connection != null)
                        connection.disconnect();

                    return null;
                }

                InputStream inputStream = null;
                try {
                    inputStream = connection.getInputStream();
                    return readResponse(inputStream);

                } catch (IOException e) {

                    Log.e("DownloadTask", e.getMessage());
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ignore) {
                        }
                    }
                    connection.disconnect();
                }

            }
            return null;
        }

        Object readResponse(InputStream in) {
            JsonReader jsonReader = null;
            Object result = null;

            try {
                jsonReader = new JsonReader(new InputStreamReader(in,"UTF-8"));
                String name;

                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    name = jsonReader.nextName();
                    switch (name) {
                        case "status":
                            String status = jsonReader.nextString();
                            if (!status.equals("success")) {
                                throw new IOException("Response message status: "+status);
                            }
                            break;

                        case "message":
                            switch (dogRequestItem.requestIndex) {
                                case BREEDS:
                                    ArrayList<String> breeds = new ArrayList<>();

                                    jsonReader.beginObject();
                                    while (jsonReader.hasNext()) {
                                        // Add the name as breed
                                        breeds.add(jsonReader.nextName());

                                        // Consume its value; in this case, an array of
                                        // sub-breeds
                                        jsonReader.beginArray();
                                        while (jsonReader.hasNext()) {
                                            jsonReader.nextString();
                                        }
                                        jsonReader.endArray();

                                    }
                                    jsonReader.endObject();
                                    result = breeds;
                                    break;

                                case BREED_ALL_IMAGES:
                                    jsonReader.beginArray();
                                    if (jsonReader.hasNext()) {
                                        result = jsonReader.nextString();

                                        // Do not need anymore data; stop reading here
                                        throw new IOException("OK");
                                    }
                                    jsonReader.endArray();
                                    break;

                                case BREED_RANDOM_IMAGE:
                                case BREED_SUB_RANDOM_IMAGE:
                                    result = jsonReader.nextString();
                                    break;

                                case BREED_LIST_SUB_BREEDS:
                                    ArrayList<String> subBreeds = new ArrayList<>(20);

                                    jsonReader.beginArray();
                                    while (jsonReader.hasNext()) {
                                        // Add the name as breed
                                        subBreeds.add(jsonReader.nextString());
                                    }
                                    jsonReader.endArray();

                                    result = new String[subBreeds.size()];
                                    subBreeds.toArray((String[])result);
                                    break;
                            }
                            break;
                    }
                }
                jsonReader.endObject();
                return result;

            } catch (IOException | IllegalStateException e) {

                String message = e.getMessage();

                if (message == null || !message.equals("OK")) {
                    Log.e("DownloadTask", "Unable to parse response using jsonReader: " + e.getMessage());
                }
            } finally {
                if (jsonReader != null) {
                    try {
                        jsonReader.close();
                    } catch (IOException ignore) {
                    }
                }
            }

            return result; // May be null
        }

        @Override
        protected void onCancelled(Object o) {
            DogContentFragment fragment = dogContentFragmentWeakReference.get();

            if (fragment != null) {
                // All finished!
                // Now clean up
                fragment.asyncTasks.remove(this);
            }
            Log.d("Download", "Cancelled");
        }

        @Override
        protected void onPostExecute(Object obj) {
            DogContentFragment fragment = dogContentFragmentWeakReference.get();

            if (fragment != null) {

                // Handle the result; ie update data, refresh list/detail views
                if (obj != null && !isCancelled()) {
                    fragment.handleResponse(dogRequestItem, obj);
                }

                // All finished!
                // Now clean up
                fragment.asyncTasks.remove(this);
            }
        }
    }
}
