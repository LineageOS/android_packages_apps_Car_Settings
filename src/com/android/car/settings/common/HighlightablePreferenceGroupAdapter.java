/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.view.View;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

/** RecyclerView adapter that supports single-preference highlighting. */
public class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {

    private int mHighlightPosition = RecyclerView.NO_POSITION;

    public HighlightablePreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
        super(preferenceGroup);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        updateBackground(holder, position);
    }

    private void updateBackground(PreferenceViewHolder holder, int position) {
        View v = holder.itemView;
        if (position == mHighlightPosition) {
            addHighlightBackground(v);
        } else if (hasHighlightBackground(v)) {
            removeHighlightBackground(v);
        }
    }

    /**
     * Requests that a particular preference be highlighted. This will remove the highlight from
     * the previously highlighted preference.
     */
    public void requestHighlight(View root, RecyclerView recyclerView, String key) {
        if (root == null || recyclerView == null) {
            return;
        }
        int position = getPreferenceAdapterPosition(key);
        if (position < 0) {
            // Item is not in the list - clearing the previous highlight without setting a new one.
            clearHighlight(root);
            return;
        }
        root.post(() -> {
            recyclerView.scrollToPosition(position);
            if (position != mHighlightPosition) {
                int oldPosition = mHighlightPosition;
                mHighlightPosition = position;
                notifyItemChanged(oldPosition);
                notifyItemChanged(position);
            }
        });
    }

    /**
     * Removes the highlight from the currently highlighted preference.
     */
    public void clearHighlight(View root) {
        if (root == null) {
            return;
        }
        root.post(() -> {
            if (mHighlightPosition < 0) {
                return;
            }
            int oldPosition = mHighlightPosition;
            mHighlightPosition = RecyclerView.NO_POSITION;
            notifyItemChanged(oldPosition);
        });
    }

    private void addHighlightBackground(View v) {
        v.setActivated(true);
    }

    private void removeHighlightBackground(View v) {
        v.setActivated(false);
    }

    private boolean hasHighlightBackground(View v) {
        return v.isActivated();
    }
}
