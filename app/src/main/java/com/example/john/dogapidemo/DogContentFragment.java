package com.example.john.dogapidemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * An interface to implement by any activity that uses DogContentFragment
 */
interface DownloadCallback {

    void updateBreeds();

    void updateDogItem_URL(int position);
}

/**
 * An interface to implement by any Detail fragment that uses DogContentFragment
 */
interface DetailDownloadCallback {
    void updateDogWithRandomImage();
}

/**
 * A headless fragment that loads the content for DOG API. It is reusable for each request.
 *
 */
public class DogContentFragment extends Fragment {

    private static final String FRAGMENT_KEY = "DogContentFragment";
    private static final String[] URLS = {
            "https://dog.ceo/api/breeds/list/all",
            "https://dog.ceo/api/breed/%s/images",
            "https://dog.ceo/api/breed/%s/images/random"
        };

    private static final int BREEDS = 0;
    private static final int BREED_ALL_IMAGES = 1;
    private static final int BREED_RANDOM_IMAGE = 2;

    private DownloadCallback downloadCallback;
    private WeakReference<DetailDownloadCallback> detailDownloadCallback;
    private ArrayList<AsyncTask> asyncTasks;

    public DogContentFragment() {
        asyncTasks = new ArrayList<>(100);
    }

    /**
     * Use DOG_API to load endpoint to list all master breeds
     */
    void loadBreeds() {
        asyncTasks.add(new RequestDogTask(new WeakReference<>(this)).execute(
                new DogRequestItem(null, BREEDS)
        ));
    }

    /**
     * Use DOG_API to load and endpoint for a graphic
     * @param dog the reference to update dog.url when task finishes
     */
    void loadBreedImagesUrl(DogItem dog) {
        asyncTasks.add(new RequestDogTask(new WeakReference<>(this)).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                new DogRequestItem(dog, BREED_ALL_IMAGES)));
    }

    void loadBreedRandomImageUrl(DogItem dog, @NonNull WeakReference<DetailDownloadCallback>
            detailDownloadCallback) {

        this.detailDownloadCallback = detailDownloadCallback;
        asyncTasks.add(new RequestDogTask(new WeakReference<>(this)).execute(
                new DogRequestItem(dog, BREED_RANDOM_IMAGE)
        ));
    }

    private void handleResponse(DogRequestItem responseItem, Object result) {

        switch (responseItem.requestIndex) {
            case BREEDS:
                if (downloadCallback != null) {
                    if (result instanceof ArrayList) {
                        ArrayList array = ((ArrayList) result);

                        for (int i = 0; i < array.size(); i++) {
                            addDogItemIfDoesNotExist((String) array.get(i));
                        }

                        downloadCallback.updateBreeds();
                        return;
                    }
                }
                break;

            case BREED_ALL_IMAGES:
                if (downloadCallback != null) {
                    assert responseItem.dogItem != null;
                    responseItem.dogItem.url = (String) result;

                    int position = findPosition(responseItem.dogItem);

                    if (position >= 0) {
                        downloadCallback.updateDogItem_URL(position);
                    }
                    return;
                }
                break;

            case BREED_RANDOM_IMAGE:
                if (detailDownloadCallback != null) {
                    DogItem dog = responseItem.dogItem;

                    assert dog != null;
                    dog.clearRandomURlFromCache();
                    dog.randomUrl = (String) result;

                    // WeakReference, to check if callback is sent to GC
                    DetailDownloadCallback callback = detailDownloadCallback.get();

                    if (callback != null) {
                        callback.updateDogWithRandomImage();
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
                assert dogRequestItem.dogItem != null;
                return String.format(URLS[index], dogRequestItem.dogItem.title.toLowerCase());

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
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DownloadCallback) {
            downloadCallback = (DownloadCallback) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadBreeds();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        downloadCallback = null;
        Log.d("Myfragment", "detach");
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

    /**
     * An array of sample (dummy) items. Items must be added synchronously
     */
    public static final List<DogItem> ITEMS = new ArrayList<>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, DogItem> ITEM_MAP = new HashMap<>();

    /**
     * <p>Wraps storing the item in both the hash map and the array list, the latter of which backs the array adapter</p>
     * <p>Continues updating the item using DOG API, if the item is not fully initialized, ie the thumbnail url</p>
     * @param title Title or breed of the dog
     */
    private void addDogItemIfDoesNotExist(String title) {

        DogItem dog;

        if ( !ITEM_MAP.containsKey(title) ) {

            dog = createDogItem(title);
            ITEM_MAP.put(dog.id, dog);
            ITEMS.add(dog);

        } else {
            dog = ITEM_MAP.get(title);
        }

        // Update item using DOG API
        if (dog.url == null) {
            loadBreedImagesUrl(dog);
        }
    }

    private DogItem createDogItem(String title) {
        String properTitle = toTitleCase(title);

        return new DogItem(title, properTitle, null);
    }

    int findPosition(DogItem dogItem) {
        return ITEMS.indexOf(dogItem);
    }

    private String toTitleCase(String word) {
        return String.valueOf(Character.toTitleCase(word.charAt(0))) +
                word.substring(1);
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class DogItem {
        /**
         * id is of form [this.title][suffix]
         * The suffix allows for multiple Dog Items with the same title
         */
        public final String id;
        /**
         * The breed of the dog is the title
         */
        String title;
        String url;
        String randomUrl;

        DogItem(String id, String title, String url) {
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
        void clearRandomURlFromCache() {
            ImageLoader loader = ImageLoader.getInstance();

            if (randomUrl != null && !randomUrl.equals(url) && loader.isInited()) {
                loader.getMemoryCache().remove(randomUrl);
            }
        }
    }

    static class DogRequestItem {
        final DogItem dogItem;
        final int requestIndex;

        DogRequestItem(@Nullable DogItem dogItem, int requestIndex) {
            this.dogItem = dogItem;
            this.requestIndex = requestIndex;
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

                // TODO: write a reader generalizing to read common structure between the different api calls
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
                                    result = jsonReader.nextString();
                                    break;
                            }
                            break;
                    }
                }
                jsonReader.endObject();
                return result;

            } catch (IOException e) {

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
