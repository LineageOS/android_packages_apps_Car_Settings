/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License.
 */
package com.android.car.settings.common;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.car.settings.CarSettingActivity;
import com.android.car.settings.R;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.util.List;

/**
 * View adapter for recycler view for tiles.
 */
public class TileRecyclerViewAdapter extends RecyclerView.Adapter<TileRecyclerViewAdapter.TileViewHolder> {
    private final DashboardCategory mHomepageCategory;
    private final Context mContext;

    public static class TileViewHolder extends ViewHolder {
        public final ImageView icon;
        public final TextView title;
        public final TextView summary;

        public TextView mTextView;

        public TileViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(android.R.id.title);
            summary = (TextView) itemView.findViewById(android.R.id.summary);
        }
    }

    private View.OnClickListener mTileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO: get rid of setTag/getTag
            ((CarSettingActivity) mContext).openTile((Tile) v.getTag());
        }
    };

    public TileRecyclerViewAdapter(Context context, DashboardCategory category) {
        mContext = context;
        mHomepageCategory = category;
    }

    @Override
    public TileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tile_item, parent, false);
        TileViewHolder vh = new TileViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(TileViewHolder holder, int position) {
        Tile tile = mHomepageCategory.tiles.get(position);
        holder.title.setText(tile.title);
        holder.icon.setImageIcon(tile.icon);
        holder.itemView.setTag(tile);
        holder.itemView.setOnClickListener(mTileClickListener);
    }

    @Override
    public int getItemCount() {
        return mHomepageCategory.tiles.size();
    }
}
