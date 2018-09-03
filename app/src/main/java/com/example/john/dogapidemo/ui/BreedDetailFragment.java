package com.example.john.dogapidemo.ui;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.john.dogapidemo.R;
import com.example.john.dogapidemo.core.DogApplication;
import com.example.john.dogapidemo.dog.api.DetailDownloadCallback;
import com.example.john.dogapidemo.dog.api.DogRepository;
import com.example.john.dogapidemo.dog.api.model.DogItem;
import com.example.john.dogapidemo.util.Util;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import java.util.List;

/**
 * A fragment representing a single Breed detail screen.
 * This fragment is either contained in a {@link BreedListActivity}
 * in two-pane mode (on tablets) or a {@link BreedDetailActivity}
 * on handsets.
 */
public class BreedDetailFragment extends Fragment implements DetailDownloadCallback {
    private DogRepository dogRepository;

    // Save State
    public static final String SUBBREED_KEY = "subbreed";

    // Fetch the fragment from Fragment Manager
    public static final String BREED_DETAIL_FRAGMENT = "Breed_Detail";

    // Manage UI and data
    public static final String LARGE_IMAGE_URI_SUFFIX = "_large";
    public static final String NO_SUBBREED = "None";

    // Data Variables
    private DogItem mItem;
    private ImageView dogPhoto;
    private String subbreed = null;

    // Helper Variables
    private SubMenu subBreedMenu;
    private static final int SUBBREED_GROUP_ID = 9;
    CollapsingToolbarLayout appBarLayout;

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BreedDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dogRepository = DogApplication.get(getContext()).fetchDogRepository();

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy title specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load title from a title provider.
            mItem = DogRepository.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }

        setHasOptionsMenu(true);
        // The fragment is retained, so it will not call onDestroy until the user navigates
        // away from the fragment
        setRetainInstance(true);

        if (savedInstanceState != null) {
            subbreed = savedInstanceState.getString(SUBBREED_KEY);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SUBBREED_KEY, subbreed);
    }

    @Override
    public void onStart() {
        super.onStart();
        dogRepository.registerDetailDownloadCallback(this);
    }

    @Override
    public void onStop() {
        dogRepository.unregisterDetailDownloadCallback(this);
        super.onStop();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Set the title
        android.app.Activity activity = this.getActivity();
        appBarLayout = activity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            appBarLayout.setTitle(makeTitleBarTitle());
        }

        View rootView = inflater.inflate(R.layout.breed_detail, container, false);
        dogPhoto = rootView.findViewById(R.id.breed_detail);

        // Show the dummy title as text in a TextView.
        if (mItem != null) {
            String url = mItem.getCurrentURL();

            if (url != null) {
                ImageLoader imageLoader = ImageLoader.getInstance();
                Bitmap largeImage = imageLoader.getMemoryCache().get(url + LARGE_IMAGE_URI_SUFFIX);

                if (largeImage != null) {
                    dogPhoto.setImageBitmap(largeImage);
                } else {

                    List<Bitmap> bitmaps = MemoryCacheUtils.findCachedBitmapsForImageUri(url, imageLoader.getMemoryCache());
                    Bitmap loadedImage = !bitmaps.isEmpty() ? bitmaps.get(0) : null;

                    // Use the cached image @ lower resolution for placeholder
                    if (loadedImage != null) {
                        dogPhoto.setImageBitmap(loadedImage);
                    }

                    // Download the image url and load into the target view
                    imageLoader.displayImage(url, dogPhoto,
                            new DisplayImageOptions.Builder()
                                .cacheInMemory(false)
                                .build(),
                            new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            ImageLoader.getInstance().getMemoryCache().put(imageUri + LARGE_IMAGE_URI_SUFFIX,
                                    loadedImage);
                        }
                    });
                }
            }
        }
        rootView.findViewById(R.id.randomPicBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (subbreed == null) {
                        dogRepository.fetchBreedRandomImageUrl(mItem.id);
                    } else {
                        dogRepository.fetchSubBreedRandomImageUrl(mItem.id, subbreed);
                    }
                }
            });

        return rootView;
    }

    @Override
    public void onDestroy() {

        // Remove the large image from the memory cache
        ImageLoader imageLoader = ImageLoader.getInstance();
        if (imageLoader.isInited() && mItem != null) {
            imageLoader.getMemoryCache().remove(mItem.getUrl() + LARGE_IMAGE_URI_SUFFIX);
            mItem.clearRandomURlFromCache();
        }
        super.onDestroy();
    }

    @Override
    public void updateDogWithRandomImage() {
        String url = mItem.getRandomUrl();

        if (url != null) {
            ImageLoader imageLoader = ImageLoader.getInstance();
            Bitmap largeImage = imageLoader.getMemoryCache().get(url + LARGE_IMAGE_URI_SUFFIX);

            if (largeImage != null) {
                dogPhoto.setImageBitmap(largeImage);
            } else {
                // Download the image url and load into the target view
                imageLoader.displayImage(url, dogPhoto,
                        new DisplayImageOptions.Builder()
                                .cacheInMemory(false)
                                .build(),
                        new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                ImageLoader.getInstance().getMemoryCache().put(imageUri + LARGE_IMAGE_URI_SUFFIX,
                                        loadedImage);
                            }
                        });
            }
        }
    }

    @Override
    public void updateSubBreedList(List<String> subBreeds) {
        addSubBreeds(subBreeds);
    }

    @Override
    public void onFailureFetchDogRandomImage() {
        Toast.makeText(getContext(), "Random image not available", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFailureFetchSubbreeds() {
        Toast.makeText(getContext(), "Subbreeds took too long to respond", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail_menu, menu);
        subBreedMenu = menu.findItem(R.id.subbreed).getSubMenu();

        List<String> subbreeds = dogRepository.fetchSubbreeds(mItem);
        if (subbreeds != null) {
            addSubBreeds(subbreeds);
        }
    }

    private void addSubBreeds(@NonNull List<String> subbreeds) {

        int id = subBreedMenu.size();
        for (String subbreed : subbreeds) {
            subBreedMenu.add(SUBBREED_GROUP_ID, id, id, Util.capitalize(subbreed));
            id++;
        }
        if (subBreedMenu.size() == 0) {
            subBreedMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, NO_SUBBREED);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == SUBBREED_GROUP_ID) {
            int position = item.getOrder();
            List<String> subbreeds = dogRepository.fetchSubbreeds(mItem);

            if (subbreeds != null) {
                subbreed = subbreeds.get(position);

                // Update toolbar Title
                if (appBarLayout != null) {
                    appBarLayout.setTitle(makeTitleBarTitle());
                }

                // Update Photo
                dogRepository.fetchSubBreedRandomImageUrl(mItem.id, subbreed);
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private String makeTitleBarTitle() {
        return subbreed != null ? mItem.getTitle() + ", " + Util.capitalize(subbreed) :
                mItem.getTitle();
    }
}
