package com.example.john.dogapidemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import java.util.List;

/**
 * A fragment representing a single Breed detail screen.
 * This fragment is either contained in a {@link BreedListActivity}
 * in two-pane mode (on tablets) or a {@link BreedDetailActivity}
 * on handsets.
 */
public class BreedDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy title this fragment is presenting.
     */
    private DogContentFragment.DogItem mItem;

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

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.title);
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // todo customize breed detail view
        View rootView = inflater.inflate(R.layout.breed_detail, container, false);

        // Show the dummy title as text in a TextView.
        if (mItem != null) {
            String url = mItem.url;
            ImageView imageView = rootView.findViewById(R.id.breed_detail);

            if (url != null) {
                ImageLoader imageLoader = ImageLoader.getInstance();
                List<Bitmap> bitmaps = MemoryCacheUtils.findCachedBitmapsForImageUri(url, imageLoader.getMemoryCache());
                Bitmap loadedImage = !bitmaps.isEmpty() ? bitmaps.get(0) : null;

                // Use the cached image for placeholder
                if (loadedImage != null) {
                    imageView.setImageBitmap(loadedImage);
                }

                // Download the image url and load into the target view
                imageLoader.displayImage(url, imageView);
            }
        }

        return rootView;
    }

    // todo create more menu options to show sub breeds
}
