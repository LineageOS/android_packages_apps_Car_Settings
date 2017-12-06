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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.car.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the content in quick setting grid view.
 */
public class QuickSettingGridAdapter extends BaseAdapter implements
        StateChangedListener {
    private Context mContext;
    private final List<Tile> tiles = new ArrayList<>();

    public QuickSettingGridAdapter(Context context) {
        mContext = context;
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

    public QuickSettingGridAdapter addTile(Tile tile) {
        tiles.add(tile);
        return this;
    }

    public void stop() {
        for (Tile tile : tiles) {
            tile.stop();
        }
    }

    @Override
    public int getCount() {
        return tiles.size();
    }

    @Override
    public Object getItem(int position) {
        return tiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View tileView = LayoutInflater.from(mContext).inflate(R.layout.tile, null);
        Tile tile = tiles.get(position);
        ImageButton icon = tileView.findViewById(R.id.tile_icon);
        icon.setImageDrawable(tile.getIcon());
        int tintColor;
        switch(tile.getState()) {
            case ON:
                tintColor = R.color.car_tint;
                break;
            case OFF:
                tintColor = R.color.car_grey_500;
                break;
            default:
                tintColor = 0;
        }
        icon.setColorFilter(
                mContext.getColor(tintColor),
                android.graphics.PorterDuff.Mode.SRC_IN);
        String textString = tile.getText();
        if (!TextUtils.isEmpty(textString)) {
            TextView text = tileView.findViewById(R.id.tile_text);
            text.setText(textString);
        }
        tileView.setOnClickListener(tile);
        return tileView;
    }

    @Override
    public void onStateChanged() {
        notifyDataSetChanged();
    }
}
