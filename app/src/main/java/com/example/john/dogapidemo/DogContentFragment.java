package com.example.john.dogapidemo;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

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

// TODO: 1) make the async tasks run at the same time
// TODO: 2) make the resume on screen rotation if they were interrupted, rather than leave the data items partially populated
/**
 * A headless fragment that loads the content for DOG API. It is reusable for each request.
 *
 */
public class DogContentFragment extends Fragment {

    private static final String FRAGMENT_KEY = "DogContentFragment";
    private static final String[] URLS = {
            "https://dog.ceo/api/breeds/list/all",
            "https://dog.ceo/api/breed/%s/images"
        };

    private static final int BREEDS = 0;
    private static final int BREED_ALL_IMAGES = 1;

    private DownloadCallback downloadCallback;

    /**
     * Use DOG_API to load endpoint to list all master breeds
     */
    void loadBreeds() {
        new RequestDogTask(new WeakReference<>(this)).execute(
                new DogRequestItem(null, BREEDS)
        );
    }

    /**
     * Use DOG_API to load and endpoint for a graphic
     * @param dog the reference to update dog.url when task finishes
     */
    void loadBreedImagesUrl(DogItem dog) {
        new RequestDogTask(new WeakReference<>(this)).execute(
                new DogRequestItem(dog, BREED_ALL_IMAGES));
    }

    private void handleResponse(DogRequestItem responseItem, Object result) {

        if (downloadCallback != null) {
            switch (responseItem.requestIndex) {
                case BREEDS:
                    if (result instanceof ArrayList) {
                        ArrayList array = ((ArrayList) result);

                        for (int i = 0, offset = ITEMS.size(); i < array.size(); i++) {
                            addItem(createDogItem(offset + i, ((String) array.get(i))));
                        }

                        downloadCallback.updateBreeds();
                    }
                    break;

                case BREED_ALL_IMAGES:
                    assert responseItem.dogItem != null;
                    responseItem.dogItem.url = (String)result;

                    downloadCallback.updateDogItem_URL(Integer.parseInt(responseItem.dogItem.id));
                    break;
            }
        } else {
            Log.d("DownloadTask", "download callback is unset; unable to handle response");
        }
    }

    private String makeURL(DogRequestItem dogRequestItem) {
        int index = dogRequestItem.requestIndex;
        switch (index) {
            case BREED_ALL_IMAGES:
                assert dogRequestItem.dogItem != null;
                return String.format(URLS[index], dogRequestItem.dogItem.title);

            default:
                return URLS[index];
        }
    }

    static DogContentFragment newInstance(FragmentManager fragmentManager) {
        DogContentFragment fragment = new DogContentFragment();
        fragmentManager.beginTransaction().add(fragment, FRAGMENT_KEY).commit();

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        downloadCallback = (DownloadCallback)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        downloadCallback = null;
    }

    /**
     * An array of sample (dummy) items.
     */
    public static final List<DogItem> ITEMS = new ArrayList<>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, DogItem> ITEM_MAP = new HashMap<>();

    /**
     * Wraps storing the item in both the hash map and the array list, the latter of which backs the array adapter
     */
    private void addItem(DogItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    private DogItem createDogItem(int position, String title) {
        DogItem dog = new DogItem(String.valueOf(position), title, "//TODO: make url");

        loadBreedImagesUrl(dog);

        return dog;
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class DogItem {
        public final String id;
        public String title;
        public String url;

        DogItem(String id, String title, String url) {
            this.id = id;
            this.title = title;
            this.url = url;
        }

        @Override
        public String toString() {
            return title;
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
        protected void onPostExecute(Object obj) {
            if (obj != null) {

                DogContentFragment fragment = dogContentFragmentWeakReference.get();

                if (fragment != null)
                    fragment.handleResponse(dogRequestItem, obj);
            }
        }
    }
}
