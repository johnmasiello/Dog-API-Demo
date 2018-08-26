package com.example.john.dogapidemo.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.john.dogapidemo.R;
import com.example.john.dogapidemo.core.DogApplication;
import com.example.john.dogapidemo.dog.api.DogRepository;
import com.example.john.dogapidemo.dog.api.DownloadCallback;
import com.example.john.dogapidemo.dog.api.model.DogItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import java.util.List;

/**
 * An activity representing a list of Breeds. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link BreedDetailActivity} representing
 * item url. On tablets, the activity presents the list of items and
 * item url side-by-side using two vertical panes.
 */
public class BreedListActivity extends AppCompatActivity implements DownloadCallback {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private SimpleItemRecyclerViewAdapter adapter;
    private int scrollPosition = 0;
    private DogRepository dogRepository;

    private final static String SCROLL_POSITION = "ScrollPos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breed_list);

        // Force the repo to be created, if not already created
        dogRepository = DogApplication.get(this).fetchDogRepository();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        if (findViewById(R.id.breed_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        DogContentFragment.getInstance(getSupportFragmentManager());

        if (savedInstanceState != null) {
            scrollPosition = savedInstanceState.getInt(SCROLL_POSITION);
        }

        RecyclerView recyclerView = findViewById(R.id.breed_list);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        dogRepository.registerDownloadCallback(this);
    }

    @Override
    protected void onStop() {
        dogRepository.unregisterDownloadCallback(this);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SCROLL_POSITION, scrollPosition);
    }

    @Override
    public void updateBreeds() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void updateDogItem_URL(int position) {
        if (adapter != null) {
            adapter.notifyItemChanged(position, DogRepository.ITEMS.get(position).getUrl());
        }
    }

    @Override
    public void reportApiFailure(Throwable throwable) {
        Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        this.adapter = new SimpleItemRecyclerViewAdapter(this, DogRepository.ITEMS, mTwoPane);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrollPosition =
                        ((LinearLayoutManager) recyclerView.getLayoutManager()).
                                findFirstCompletelyVisibleItemPosition();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        recyclerView.getLayoutManager().scrollToPosition(scrollPosition);
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final AppCompatActivity mParentActivity;
        private final List<DogItem> mValues;
        private final boolean mTwoPane;


        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DogItem item = (DogItem) view.getTag();

                if (view.getId() == R.id.rating_bar) {
                    // Consume the click
                    return;
                }

                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(BreedDetailFragment.ARG_ITEM_ID, item.id);
                    BreedDetailFragment fragment = new BreedDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.breed_detail_container, fragment, BreedDetailFragment.BREED_DETAIL_FRAGMENT)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, BreedDetailActivity.class);
                    intent.putExtra(BreedDetailFragment.ARG_ITEM_ID, item.id);

                    context.startActivity(intent);
                }
            }
        };

        private final RatingBar.OnRatingBarChangeListener mOnRatingBarChangeListener = new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (fromUser) {
                    ((DogItem) ratingBar.getTag()).setRating(rating);
                }
            }
        };

        SimpleItemRecyclerViewAdapter(AppCompatActivity parent,
                                      List<DogItem> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.breed_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mIdView.setText(mValues.get(position).getTitle());
            holder.mRatingBar.setRating(mValues.get(position).getRating());

            String url = mValues.get(position).getUrl();
            updateThumbnail(holder, url);

            // Set the data item to the view's holder
            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
            holder.mRatingBar.setOnRatingBarChangeListener(mOnRatingBarChangeListener);
            holder.mRatingBar.setTag(mValues.get(position));
        }

        private void updateThumbnail(ViewHolder holder, String url) {
            if (url != null) {
                ImageLoader imageLoader = ImageLoader.getInstance();
                List<Bitmap> bitmaps = MemoryCacheUtils.findCachedBitmapsForImageUri(url, imageLoader.getMemoryCache());
                Bitmap loadedImage = !bitmaps.isEmpty() ? bitmaps.get(0) : null;

                // Use the cached image
                if (loadedImage != null) {
                    holder.mContentView.setImageBitmap(loadedImage);
                } else {
                    // Download the image url and load into the target view
                    DisplayImageOptions thumbOptions = new DisplayImageOptions.Builder()
                            .cacheInMemory(true)
                            .imageScaleType(ImageScaleType.EXACTLY) // Sample the image to thumbnail size to reduce memory
                            .bitmapConfig(Bitmap.Config.RGB_565)
                            .build();

                    imageLoader.displayImage(
                            url,
                            new ImageViewAware(holder.mContentView, false),
                            thumbOptions);
                }
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {

                Object payload = payloads.get(0);

                if (payload instanceof String) {
                    updateThumbnail(holder, (String) payload);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mIdView;
            final ImageView mContentView;
            final RatingBar mRatingBar;

            ViewHolder(View view) {
                super(view);
                mIdView = view.findViewById(R.id.item_name);
                mContentView = view.findViewById(R.id.item_thumb);
                mRatingBar = view.findViewById(R.id.rating_bar);
            }
        }
    }
}
