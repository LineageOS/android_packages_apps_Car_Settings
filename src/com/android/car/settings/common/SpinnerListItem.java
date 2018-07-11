/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.car.widget.ListItem;

import com.android.car.settings.R;

import java.util.List;

/**
 * Represents a listItem with title text and a checkbox.
 * @param <T> type of item stored in the spinner.
 */
public class SpinnerListItem<T> extends ListItem<SpinnerListItem.ViewHolder> {
    private final ArrayAdapter<T> mArrayAdapter;
    private final AdapterView.OnItemSelectedListener mOnItemSelectedListener;
    private final CharSequence mTitle;
    private final int mSelectedPosition;
    private boolean mIsEnabled = true;

    /**
     * Constructs a new SpinnerLineItem
     *
     * @param context Android context
     * @param listener Listener for when an item in spinner is selected
     * @param items The List of items in the spinner
     * @param title The title next to the spinner
     * @param selectedPosition The starting position of the spinner
     */
    public SpinnerListItem(
            Context context,
            AdapterView.OnItemSelectedListener listener,
            List<T> items,
            CharSequence title,
            int selectedPosition) {
        mArrayAdapter = new ArrayAdapter(context, R.layout.spinner, items);
        mArrayAdapter.setDropDownViewResource(R.layout.spinner_drop_down);
        mOnItemSelectedListener = listener;
        mTitle = title;
        mSelectedPosition = selectedPosition;
    }

    @LayoutRes
    public static final int getViewLayoutId() {
        return R.layout.spinner_line_item;
    }

    /**
     * Creates a {@link SpinnerListItem.ViewHolder}.
     */
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public final int getViewType() {
        return CustomListItemTypes.SPINNER_VIEW_TYPE;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    @Override
    protected void resolveDirtyState() {
        // nothing to resolve.
    }

    @Override
    protected void onBind(ViewHolder viewHolder) {
        viewHolder.spinner.setAdapter(mArrayAdapter);
        viewHolder.spinner.setSelection(mSelectedPosition);
        viewHolder.spinner.setOnItemSelectedListener(mOnItemSelectedListener);
        viewHolder.spinner.setEnabled(mIsEnabled);
        viewHolder.titleView.setText(mTitle);
        viewHolder.titleView.setEnabled(mIsEnabled);
        viewHolder.itemView.setEnabled(mIsEnabled);
    }

    /**
     * Returns the item in the given position
     */
    public T getItem(int position) {
        return mArrayAdapter.getItem(position);
    }

    static class ViewHolder extends ListItem.ViewHolder {
        public final Spinner spinner;
        public final TextView titleView;

        ViewHolder(View view) {
            super(view);
            spinner = view.findViewById(R.id.spinner);
            titleView = view.findViewById(R.id.title);
        }

        @Override
        public void applyUxRestrictions(@NonNull CarUxRestrictions restrictions) {
            // no-op
        }
    }
}
