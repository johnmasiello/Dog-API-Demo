package com.example.john.dogapidemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;
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
    private DogContentFragment dogContentFragment;
    private SimpleItemRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breed_list);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .denyCacheImageMultipleSizesInMemory()
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(config);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if (findViewById(R.id.breed_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        dogContentFragment = DogContentFragment.newInstance(getSupportFragmentManager());
        dogContentFragment.loadBreeds();

        RecyclerView recyclerView = findViewById(R.id.breed_list);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);
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
            adapter.notifyItemChanged(position, DogContentFragment.ITEMS.get(position).url);
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        int width = getResources().getDimensionPixelSize(R.dimen.thumbWidth);

        //noinspection SuspiciousNameCombination
        this.adapter = new SimpleItemRecyclerViewAdapter(this, DogContentFragment.ITEMS, mTwoPane,
                new Rect(0,0,width,width));
        recyclerView.setAdapter(adapter);
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final BreedListActivity mParentActivity;
        private final List<DogContentFragment.DogItem> mValues;
        private final boolean mTwoPane;
        private final Rect mIconRect;

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DogContentFragment.DogItem item = (DogContentFragment.DogItem) view.getTag();
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(BreedDetailFragment.ARG_ITEM_ID, item.id);
                    BreedDetailFragment fragment = new BreedDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.breed_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, BreedDetailActivity.class);
                    intent.putExtra(BreedDetailFragment.ARG_ITEM_ID, item.id);

                    context.startActivity(intent);
                }
            }
        };

        SimpleItemRecyclerViewAdapter(BreedListActivity parent,
                                      List<DogContentFragment.DogItem> items,
                                      boolean twoPane, Rect iconBoundary) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
            mIconRect = iconBoundary;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.breed_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            // todo customize binding data to the views in the list item
            holder.mIdView.setText(mValues.get(position).title);

            String url = mValues.get(position).url;
            updateThumbnail(holder, url);

            // Set the data item to the view's holder
            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        private void updateThumbnail(ViewHolder holder, final String url) {
            if (url != null) {
                ImageLoader imageLoader = ImageLoader.getInstance();
                List<Bitmap> bitmaps = MemoryCacheUtils.findCachedBitmapsForImageUri(url, imageLoader.getMemoryCache());
                Bitmap loadedImage = !bitmaps.isEmpty() ? bitmaps.get(0) : null;

                // Use the cached image
                if (loadedImage != null) {
                    holder.mContentView.setImageBitmap(loadedImage);
                    Log.d("Thumbnail", "using cached image");
                } else {
                    Log.d("Thumbnail", "fetching image: "+url);

                    final Rect thumb = holder.thumbRect;

                    // Download the image url and load into the target view
                    DisplayImageOptions thumbOptions = new DisplayImageOptions.Builder()
                            .cacheInMemory(true)
                            .preProcessor(new BitmapProcessor() { // Resample the image to icon size to save memory/ use less cache
                                @Override
                                public Bitmap process(Bitmap bitmap) {
                                    Bitmap dst = Bitmap.createBitmap(mIconRect.width(), mIconRect.height(), bitmap.getConfig());

                                    Canvas canvas = new Canvas(dst);
                                    canvas.drawBitmap(bitmap,
                                            findSourceCrop(thumb, bitmap.getWidth(), bitmap.getHeight())
                                            , mIconRect, null);
                                    return dst;
                                }

                                private Rect findSourceCrop(Rect src, int width, int height) {
                                    // Algorithm to crop a square from the source bitmap, given its width and height

                                    int inset;

                                    if (width > height) {
                                        // Cut the width,
                                        // by rounding the edges out

                                        // Round the inset down
                                        inset = (width - height) / 2;
                                        src.set(inset, 0, width - inset, height);
                                    } else {
                                        // In similar fashion, cut the height
                                        inset = (height - width) / 2;
                                        src.set(0, inset, width, height - inset);
                                    }
                                    return src;
                                }
                            })
                            .build();

                    imageLoader.displayImage(url, holder.mContentView,
                            thumbOptions);
                }
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {

                Object payload = payloads.get(payloads.size() - 1);

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
            // todo customize to handle the view outlets of the list item layout
            final TextView mIdView;
            final ImageView mContentView;

            final Rect thumbRect;

            ViewHolder(View view) {
                super(view);
                mIdView = view.findViewById(R.id.item_name);
                mContentView = view.findViewById(R.id.item_thumb);

                thumbRect = new Rect();
            }
        }
    }
}
