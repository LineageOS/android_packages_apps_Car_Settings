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
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.car.widget.ListItem;

import com.android.car.settings.R;

/**
 * Represents a listItem with title text and a checkbox.
 */
public abstract class CheckBoxListItem extends ListItem<CheckBoxListItem.ViewHolder> {
    private final String mTitle;
    private boolean mIsEnabled = true;

    public CheckBoxListItem(String title) {
        mTitle = title;
    }

    /**
     * @return whether the CheckBox is checked
     */
    public abstract boolean isChecked();

    @LayoutRes
    public static final int getViewLayoutId() {
        return R.layout.checkbox_list_item;
    }

    /**
     * Creates a {@link CheckBoxListItem.ViewHolder}.
     */
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public final int getViewType() {
        return CustomListItemTypes.CHECK_BOX_VIEW_TYPE;
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
        viewHolder.titleView.setText(mTitle);
        viewHolder.titleView.setEnabled(mIsEnabled);
        viewHolder.checkbox.setChecked(isChecked());
        viewHolder.checkbox.setEnabled(mIsEnabled);
        viewHolder.itemView.setOnClickListener(this::onClick);
        viewHolder.itemView.setEnabled(mIsEnabled);
    }

    /**
     * Called when the ListItem is clicked, default behavior is nothing.
     *
     * @param view Passed in by the view's onClick listener
     */
    public void onClick(View view) {
        // do nothing
    }

    static class ViewHolder extends ListItem.ViewHolder {
        public final TextView titleView;
        public final CheckBox checkbox;

        ViewHolder(View view) {
            super(view);
            titleView = view.findViewById(R.id.title);
            checkbox = view.findViewById(R.id.checkbox);
        }

        @Override
        public void applyUxRestrictions(@NonNull CarUxRestrictions restrictions) {
            // no-op
        }
    }
}
