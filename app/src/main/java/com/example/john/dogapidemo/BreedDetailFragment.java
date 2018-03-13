package com.example.john.dogapidemo;

import android.graphics.Bitmap;
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

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A fragment representing a single Breed detail screen.
 * This fragment is either contained in a {@link BreedListActivity}
 * in two-pane mode (on tablets) or a {@link BreedDetailActivity}
 * on handsets.
 */
public class BreedDetailFragment extends Fragment implements DetailDownloadCallback {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    public static final String SUBBREED_KEY = "subbreed";

    public static final String LARGE_IMAGE_URI_SUFFIX = "_large";

    public static final String BREED_DETAIL_FRAGMENT = "Breed_Detail";

    public static final String NO_SUBBREED = "None";

    // Data Variables
    private DogContentFragment.DogItem mItem;
    private ImageView dogPhoto;
    private String[] subBreeds;

    // Helper Variables
    private SubMenu subBreedMenu;
    private static final int SUBBREED_GROUP_ID = 9;
    CollapsingToolbarLayout appBarLayout;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BreedDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy title specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load title from a title provider.
            mItem = DogContentFragment.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }

        setHasOptionsMenu(true);
        // The fragment is retained, so it will not call onDestroy until the user navigates
        // away from the fragment
        setRetainInstance(true);

        if (savedInstanceState != null) {
            subBreeds = savedInstanceState.getStringArray(SUBBREED_KEY);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray(SUBBREED_KEY, subBreeds);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Set the title
        android.app.Activity activity = this.getActivity();
        appBarLayout = activity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            appBarLayout.setTitle(mItem.title);

            final DetailDownloadCallback ddc = this;
            DogContentFragment fragment = DogContentFragment.getInstance(getFragmentManager());

            if (fragment != null && savedInstanceState == null) {
                fragment.loadSubBreedList(mItem, new WeakReference<>(ddc));
            }
        }


        // todo customize breed detail view
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

        final DetailDownloadCallback ddc = this;
        rootView.findViewById(R.id.randomPicBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DogContentFragment fragment = DogContentFragment.getInstance(getFragmentManager());

                if (fragment != null) {
                    fragment.loadBreedRandomImageUrl(mItem, new WeakReference<>(ddc));
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
            imageLoader.getMemoryCache().remove(mItem.url + LARGE_IMAGE_URI_SUFFIX);
            mItem.clearRandomURlFromCache();
        }
        super.onDestroy();
    }

    @Override
    public void updateDogWithRandomImage() {
        String url = mItem.randomUrl;

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
    public void updateSubBreedList(String[] subBreeds) {
        this.subBreeds = subBreeds;
        addSubBreeds();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail_menu, menu);
        subBreedMenu = menu.findItem(R.id.subbreed).getSubMenu();

        if (subBreeds != null) {
            addSubBreeds();
        }
    }

    private void addSubBreeds() {

        int id = subBreedMenu.size();
        for (String subBreed : subBreeds) {
            subBreedMenu.add(SUBBREED_GROUP_ID, id, id, DogContentFragment.makeTitleCase(subBreed));
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
            String subbreed = subBreeds[position];

            // Update toolbar Title
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.title+ ", " +
                        DogContentFragment.makeTitleCase(subbreed));
            }

            // Update Photo
            DogContentFragment fragment = DogContentFragment.getInstance(getFragmentManager());

            if (fragment != null) {
                fragment.loadSubBreedRandomImageUrl(mItem, subbreed,
                        new WeakReference<DetailDownloadCallback>(this));
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
