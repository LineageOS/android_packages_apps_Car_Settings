/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.android.car.settings.quicksettings;

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.car.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the content in quick setting grid view.
 */
public class QuickSettingGridAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements StateChangedListener {
    private static final int COLUMN_COUNT = 4;
    // alpha value for icon or text.
    private static final int ALPHA_DISABLED = 128;
    private static final int ALPHA_ENABLED = 255;
    private static final int SEEKBAR_VIEWTYPE = 0;
    private static final int TILE_VIEWTYPE = 1;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final List<Tile> mTiles = new ArrayList<>();
    private final List<SeekbarTile> mSeekbarTiles = new ArrayList<>();
    private final QsSpanSizeLookup mQsSpanSizeLookup = new QsSpanSizeLookup();

    public QuickSettingGridAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    GridLayoutManager getGridLayoutManager() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext,
                mContext.getResources().getInteger(R.integer.quick_setting_column_count));
        gridLayoutManager.setSpanSizeLookup(mQsSpanSizeLookup);
        return gridLayoutManager;
    }

    /**
     * Represents an UI tile in the quick setting grid.
     */
    interface Tile extends View.OnClickListener {

        /**
         * A state to indicate how we want to render icon, this is independent of what to show
         * in text.
         */
        enum State {OFF, ON}

        /**
         * Called when activity owning this tile's onStop() gets called.
         */
        void stop();

        Drawable getIcon();

        @Nullable
        String getText();

        State getState();
    }

    interface SeekbarTile extends SeekBar.OnSeekBarChangeListener {
        /**
         * Called when activity owning this tile's onStop() gets called.
         */
        void stop();

        int getMax();

        int getCurrent();
    }

    QuickSettingGridAdapter addSeekbarTile(SeekbarTile seekbarTile) {
        mSeekbarTiles.add(seekbarTile);
        return this;
    }

    QuickSettingGridAdapter addTile(Tile tile) {
        mTiles.add(tile);
        return this;
    }

    void stop() {
        for (SeekbarTile tile : mSeekbarTiles) {
            tile.stop();
        }
        for (Tile tile : mTiles) {
            tile.stop();
        }
        mTiles.clear();
        mSeekbarTiles.clear();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case SEEKBAR_VIEWTYPE:
                return new BrightnessViewHolder(mInflater.inflate(
                        R.layout.brightness_tile, parent, /* attachToRoot= */ false));
            case TILE_VIEWTYPE:
                return new TileViewHolder(mInflater.inflate(
                        R.layout.tile, parent, /* attachToRoot= */ false));
            default:
                throw new RuntimeException("unknown viewType: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case SEEKBAR_VIEWTYPE:
                SeekbarTile seekbarTile = mSeekbarTiles.get(position);
                SeekBar seekbar = ((BrightnessViewHolder) holder).mSeekBar;
                seekbar.setMax(seekbarTile.getMax());
                seekbar.setProgress(seekbarTile.getCurrent());
                seekbar.setOnSeekBarChangeListener(seekbarTile);
                break;
            case TILE_VIEWTYPE:
                Tile tile = mTiles.get(position - mSeekbarTiles.size());
                TileViewHolder vh = (TileViewHolder) holder;
                vh.mIcon.setImageDrawable(tile.getIcon());
                switch (tile.getState()) {
                    case ON:
                        vh.mIcon.setAlpha(ALPHA_ENABLED);
                        vh.mText.setAlpha(ALPHA_ENABLED);
                        break;
                    case OFF:
                        vh.mIcon.setAlpha(ALPHA_DISABLED);
                        vh.mText.setAlpha(ALPHA_DISABLED);
                        break;
                    default:
                }
                String textString = tile.getText();
                if (!TextUtils.isEmpty(textString)) {
                    vh.mText.setText(textString);
                }
                vh.itemView.setOnClickListener(tile);
                break;
            default:
        }
    }

    private class BrightnessViewHolder extends RecyclerView.ViewHolder {
        private final SeekBar mSeekBar;

        BrightnessViewHolder(View view) {
            super(view);
            mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
        }
    }

    private class TileViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mText;

        TileViewHolder(View view) {
            super(view);
            mIcon = (ImageView) view.findViewById(R.id.tile_icon);
            mText = (TextView) view.findViewById(R.id.tile_text);
        }
    }

    class QsSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

        /**
         * Each line item takes a full row, and each tile takes only 1 span.
         */
        @Override
        public int getSpanSize(int position) {
            return position < mSeekbarTiles.size() ? COLUMN_COUNT : 1;
        }

        @Override
        public int getSpanIndex(int position, int spanCount) {
            return position < mSeekbarTiles.size()
                    ? 1 : (position - mSeekbarTiles.size()) % COLUMN_COUNT;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position < mSeekbarTiles.size() ? SEEKBAR_VIEWTYPE : TILE_VIEWTYPE;
    }

    @Override
    public int getItemCount() {
        return mTiles.size() + mSeekbarTiles.size();
    }

    @Override
    public void onStateChanged() {
        notifyDataSetChanged();
    }
}
