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

package com.android.car.settings.suggestions;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.car.widget.ListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.CustomListItemTypes;

/**
 * Represents suggestion list item.
 */
public class SuggestionListItem extends ListItem<SuggestionListItem.ViewHolder> {
    private final CharSequence mTitle;
    private final CharSequence mPrimaryAction;
    private final CharSequence mSecondaryAction;
    private final Drawable mIconDrawable;
    private final CharSequence mSummary;
    private final View.OnClickListener mOnClickListener;
    private final ActionListener mActionListener;

    private boolean mIsEnabled = true;

    /**
     * Creates a {@link SuggestionListItem} with title, summary, icons, and click handlers.
     */
    public SuggestionListItem(
            CharSequence title,
            CharSequence summary,
            Drawable iconDrawable,
            CharSequence primaryAction,
            CharSequence secondaryAction,
            View.OnClickListener onClickListener,
            ActionListener actionListener) {
        mTitle = title;
        mPrimaryAction = primaryAction;
        mSecondaryAction = secondaryAction;
        mIconDrawable = iconDrawable;
        mSummary = summary;
        mOnClickListener = onClickListener;
        mActionListener = actionListener;
    }

    @LayoutRes
    public static final int getViewLayoutId() {
        return R.layout.action_icon_button_list_item;
    }

    @Override
    protected void onBind(ViewHolder viewHolder) {
        viewHolder.mActionButton1.setText(mPrimaryAction);
        viewHolder.mActionButton1.setEnabled(mIsEnabled);
        viewHolder.mActionButton2.setText(mSecondaryAction);
        viewHolder.mActionButton2.setEnabled(mIsEnabled);
        viewHolder.mTitleView.setText(mTitle);
        viewHolder.mTitleView.setEnabled(mIsEnabled);
        viewHolder.mEndIconView.setImageDrawable(mIconDrawable);
        viewHolder.mEndIconView.setEnabled(mIsEnabled);
        viewHolder.mDescView.setEnabled(mIsEnabled);
        viewHolder.itemView.setEnabled(mIsEnabled);
        if (TextUtils.isEmpty(mSummary)) {
            viewHolder.mDescView.setVisibility(View.GONE);
        } else {
            viewHolder.mDescView.setVisibility(View.VISIBLE);
            viewHolder.mDescView.setText(mSummary);
        }
        viewHolder.mActionButton2.setOnClickListener(
                v -> onSecondaryActionButtonClick(viewHolder.getAdapterPosition()));

        viewHolder.mActionButton1.setOnClickListener(
                v -> onPrimaryActionButtonClick(viewHolder.mView));
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
    public final int getViewType() {
        return CustomListItemTypes.SUGGESTION_VIEW_TYPE;
    }

    /**
     * Creates a {@link ViewHolder}.
     */
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    private void onSecondaryActionButtonClick(int adapterPosition) {
        mActionListener.onSuggestionItemDismissed(adapterPosition);
    }

    private void onPrimaryActionButtonClick(View view) {
        mOnClickListener.onClick(view);
    }

    /**
     * ViewHolder that contains the elements that make up an ActionIconButtonListItem,
     * including the title, description, icon, end action button, and divider.
     */
    public static class ViewHolder extends ListItem.ViewHolder {
        final TextView mTitleView;
        final TextView mDescView;
        final ImageView mEndIconView;
        final Button mActionButton1;
        final Button mActionButton2;
        final View mView;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mEndIconView = (ImageView) mView.findViewById(R.id.end_icon);
            mTitleView = (TextView) mView.findViewById(R.id.title);
            mDescView = (TextView) mView.findViewById(R.id.desc);
            mActionButton1 = (Button) mView.findViewById(R.id.action_button_1);
            mActionButton2 = (Button) mView.findViewById(R.id.action_button_2);
        }

        @Override
        public void onUxRestrictionsChanged(CarUxRestrictions restrictions) {
            // no-op
        }
    }

    /**
     * Interface that surfaces events on the suggestion.
     */
    public interface ActionListener {

        /**
         * Invoked when a suggestions item is dismissed.
         *
         * @param adapterPosition the position of the suggestion item in it's adapter.
         */
        void onSuggestionItemDismissed(int adapterPosition);
    }
}
