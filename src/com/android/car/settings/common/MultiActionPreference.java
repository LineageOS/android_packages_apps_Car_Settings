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

import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.R;
import com.android.car.ui.preference.CarUiPreference;
import com.android.car.ui.uxr.DrawableStateImageView;

/**
 * A class for preferences that have a main click action along with up to two group of actions that
 * can be displayed separately. One group of actions consists of two action items and the other of
 * one action item.
 */
public class MultiActionPreference extends CarUiPreference
        implements BaseActionItem.ActionItemInfoChangeListener {

    /**
     * Identifier enum for the second group which consists of two different action items.
     */
    public enum ActionItemGroupTwo {
        ACTION_ITEM1,
        ACTION_ITEM2,
    }

    /**
     * Identifier enum for the first group which consists of one action items.
     */
    public enum ActionItemGroupOne {
        ACTION_ITEM1,
    }

    @Nullable
    private Integer mSummaryColor;

    private boolean mShowChevron;

    protected BaseActionItem[] mActionItemArrayGroupOne = new BaseActionItem[1];
    protected BaseActionItem[] mActionItemArrayGroupTwo = new BaseActionItem[2];

    public MultiActionPreference(Context context,
            AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public MultiActionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public MultiActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MultiActionPreference(Context context) {
        super(context);
        init(null);
    }

    /**
     * Initialize styles and attributes, as well as what types of action items should be created.
     *
     * @param attrs Attribute set from which to read values
     */
    @CallSuper
    protected void init(@Nullable AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.MultiActionPreference);

        try {

            // Single case switch statement for now with the assumption more ActionItems will be
            // added in the future
            switch (a.getInt(
                    R.styleable.MultiActionPreference_group_one_action_item_one, -1)) {
                case 0:
                    mActionItemArrayGroupOne[0] = new DrawableButtonActionItem(this);
                    break;
                default:
                    break;
            }

            setGroupOneVisibleAndEnableState(a);

            // Single case switch statement for now with the assumption more ActionItems will be
            // added in the future
            switch (a.getInt(
                    R.styleable.MultiActionPreference_group_two_action_item_one, -1)) {
                case 0:
                    mActionItemArrayGroupTwo[0] = new ToggleButtonActionItem(this);
                    break;
                default:
                    break;
            }

            switch (a.getInt(
                    R.styleable.MultiActionPreference_group_two_action_item_two, -1)) {
                case 0:
                    mActionItemArrayGroupTwo[1] = new ToggleButtonActionItem(this);
                    break;
                default:
                    break;
            }

            setGroupTwoVisibleAndEnableState(a);
        } finally {
            a.recycle();
        }

        setLayoutResource(R.layout.multi_action_preference);
    }

    private void setGroupOneVisibleAndEnableState(TypedArray styledAttributes) {
        int[] actionItemVisibility = {
                R.styleable.MultiActionPreference_group_one_action_item_one_shown};
        int[] actionItemEnabled = {
                R.styleable.MultiActionPreference_group_one_action_item_one_enabled};

        setGroupVisibleAndEnableState(mActionItemArrayGroupOne, actionItemVisibility,
                actionItemEnabled, styledAttributes);
    }

    private void setGroupTwoVisibleAndEnableState(TypedArray styledAttributes) {
        int[] actionItemVisibility = {
                R.styleable.MultiActionPreference_group_two_action_item_one_shown,
                R.styleable.MultiActionPreference_group_two_action_item_two_shown};
        int[] actionItemEnabled = {
                R.styleable.MultiActionPreference_group_two_action_item_one_enabled,
                R.styleable.MultiActionPreference_group_two_action_item_two_enabled};

        setGroupVisibleAndEnableState(mActionItemArrayGroupTwo, actionItemVisibility,
                actionItemEnabled, styledAttributes);
    }

    private void setGroupVisibleAndEnableState(BaseActionItem[] items, int[] actionItemVisibility,
            int[] actionItemEnabled, TypedArray styledAttributes) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                items[i].setVisible(styledAttributes.getBoolean(
                        actionItemVisibility[i], true));
                items[i].setEnabled(styledAttributes.getBoolean(
                        actionItemEnabled[i], true));
            }
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (mSummaryColor != null) {
            TextView summary = requireViewByRefId(holder.itemView, android.R.id.summary);
            summary.setTextColor(mSummaryColor);
        }

        DrawableStateImageView chevronIcon = requireViewByRefId(holder.itemView, R.id.icon_chevron);
        chevronIcon.setVisibility(mShowChevron ? View.VISIBLE : View.INVISIBLE);

        View actionContainerGroupOne = requireViewByRefId(holder.itemView,
                R.id.multi_action_preference_second_action_container1);
        FrameLayout container0 = requireViewByRefId(holder.itemView,
                R.id.multi_action_preference_item_container0);

        View actionContainerGroupTwo = requireViewByRefId(holder.itemView,
                R.id.multi_action_preference_second_action_container2);
        FrameLayout container1 = requireViewByRefId(holder.itemView,
                R.id.multi_action_preference_item_container1);
        FrameLayout container2 = requireViewByRefId(holder.itemView,
                R.id.multi_action_preference_item_container2);

        setGroupVisibilityStateAndListeners(mActionItemArrayGroupOne, actionContainerGroupOne);
        setGroupVisibilityStateAndListeners(mActionItemArrayGroupTwo, actionContainerGroupTwo);

        if (mActionItemArrayGroupOne[0] != null) {
            mActionItemArrayGroupOne[0].bindViewHolder(container0);
        }

        if (mActionItemArrayGroupTwo[0] != null) {
            mActionItemArrayGroupTwo[0].bindViewHolder(container1);
        }

        if (mActionItemArrayGroupTwo[1] != null) {
            mActionItemArrayGroupTwo[1].bindViewHolder(container2);
        }

    }

    private void setGroupVisibilityStateAndListeners(BaseActionItem[] items, View groupContainer) {
        boolean isActionContainerGroupVisible = false;
        for (BaseActionItem baseActionItem : items) {
            if (baseActionItem != null) {
                baseActionItem.setPreference(this)
                        .setRestrictedOnClickListener(getOnClickWhileRestrictedListener());

                if (baseActionItem.isVisible()) {
                    isActionContainerGroupVisible = true;
                }
            }
        }

        groupContainer.setVisibility(
                isActionContainerGroupVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onActionItemChange(BaseActionItem baseActionItem) {
        notifyChanged();
    }

    /**
     * Gets color of the summary text
     */
    public Integer getSummaryColor() {
        return mSummaryColor;
    }

    /**
     * Sets color of the summary text
     */
    public void setSummaryColor(Integer summaryColorResId) {
        mSummaryColor = summaryColorResId;
        notifyChanged();
    }

    /**
     * Shows or hides chevron icon
     */
    public void showChevron(boolean show) {
        mShowChevron = show;
        notifyChanged();
    }

    /**
     * Retrieve the specified BaseActionItem based on the index in the first group of actions
     */
    public BaseActionItem getGroupOneActionItem(ActionItemGroupOne actionItem) {
        switch (actionItem) {
            case ACTION_ITEM1:
                return mActionItemArrayGroupOne[0];
            default:
                throw new IllegalArgumentException("Invalid button requested");
        }
    }

    /**
     * Retrieve the specified BaseActionItem based on the index in the second group of actions
     */
    public BaseActionItem getGroupTwoActionItem(ActionItemGroupTwo actionItem) {
        switch (actionItem) {
            case ACTION_ITEM1:
                return mActionItemArrayGroupTwo[0];
            case ACTION_ITEM2:
                return mActionItemArrayGroupTwo[1];
            default:
                throw new IllegalArgumentException("Invalid button requested");
        }
    }
}
