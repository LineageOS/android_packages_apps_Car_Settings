/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;
import com.android.car.ui.preference.CarUiTwoActionBasePreference;
import com.android.settingslib.Utils;

import java.util.function.Consumer;

/**
 * Extends {@link CarUiTwoActionBasePreference} to add in a colored action text.
 */
public class ColoredTwoActionSwitchPreference extends CarUiTwoActionBasePreference {
    @Nullable
    protected Consumer<Boolean> mSecondaryActionOnClickListener;
    private boolean mSecondaryActionChecked;
    private ColorStateList mWarningTextColor;
    private ColorStateList mNormalTextColor;
    private boolean mIsWarning;
    private CharSequence mActionText;

    public ColoredTwoActionSwitchPreference(Context context,
            AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ColoredTwoActionSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ColoredTwoActionSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColoredTwoActionSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected void init(@Nullable AttributeSet attrs) {
        super.init(attrs);

        setLayoutResourceInternal(R.layout.colored_two_action_switch_preference);
        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.ColoredTwoActionSwitchPreference);
        mWarningTextColor = a.getColorStateList(
                R.styleable.ColoredTwoActionSwitchPreference_warningTextColor);
        if (mWarningTextColor == null) {
            mWarningTextColor = Utils.getColorAttr(getContext(), android.R.attr.textColorPrimary);
        }

        mNormalTextColor = a.getColorStateList(
                R.styleable.ColoredTwoActionSwitchPreference_normalTextColor);
        if (mNormalTextColor == null) {
            mNormalTextColor = Utils.getColorAttr(getContext(), android.R.attr.textColorPrimary);
        }
    }

    @Override
    protected void performSecondaryActionClickInternal() {
        if (isSecondaryActionEnabled()) {
            if (isUxRestricted()) {
                Consumer<Preference> restrictedListener = getOnClickWhileRestrictedListener();
                if (restrictedListener != null) {
                    restrictedListener.accept(this);
                }
            } else {
                mSecondaryActionChecked = !mSecondaryActionChecked;
                notifyChanged();
                if (mSecondaryActionOnClickListener != null) {
                    mSecondaryActionOnClickListener.accept(mSecondaryActionChecked);
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View firstActionContainer = requireViewByRefId(holder.itemView,
                R.id.colored_preference_first_action_container);
        View secondActionContainer = requireViewByRefId(holder.itemView,
                R.id.colored_preference_second_action_container);
        View secondaryAction = requireViewByRefId(holder.itemView,
                R.id.colored_preference_secondary_action);
        Switch s = requireViewByRefId(holder.itemView,
                R.id.colored_preference_secondary_action_concrete);

        holder.itemView.setFocusable(false);
        holder.itemView.setClickable(false);
        firstActionContainer.setOnClickListener(this::performClick);
        firstActionContainer.setEnabled(isEnabled() || isUxRestricted());
        firstActionContainer.setFocusable(isEnabled() || isUxRestricted());

        secondActionContainer.setVisibility(mSecondaryActionVisible ? View.VISIBLE : View.GONE);
        s.setChecked(mSecondaryActionChecked);
        s.setEnabled(isSecondaryActionEnabled());

        secondaryAction.setOnClickListener(v -> performSecondaryActionClickInternal());
        secondaryAction.setEnabled(isSecondaryActionEnabled() || isUxRestricted());
        secondaryAction.setFocusable(isSecondaryActionEnabled() || isUxRestricted());


        TextView actionTextView = holder.itemView.findViewById(R.id.action_text);
        if (actionTextView != null) {
            if (!TextUtils.isEmpty(mActionText)) {
                actionTextView.setText(mActionText);
                actionTextView.setVisibility(View.VISIBLE);
                if (getIsWarning()) {
                    actionTextView.setTextColor(mWarningTextColor);
                } else {
                    actionTextView.setTextColor(mNormalTextColor);
                }
            } else {
                actionTextView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Sets the checked state of the switch in the secondary action space.
     * @param checked Whether the switch should be checked or not.
     */
    @VisibleForTesting
    public void setSecondaryActionChecked(boolean checked) {
        mSecondaryActionChecked = checked;
        notifyChanged();
    }

    /**
     * Sets the on-click listener of the secondary action button.
     *
     * The listener is called with the current checked state of the switch.
     */
    public void setOnSecondaryActionClickListener(@Nullable Consumer<Boolean> onClickListener) {
        mSecondaryActionOnClickListener = onClickListener;
        notifyChanged();
    }

    /**
     * Get the category of the action text
     */
    public boolean getIsWarning() {
        return mIsWarning;
    }

    /**
     * Set whether action text is a warning text
     */
    public void setIsWarning(boolean isWarning) {
        mIsWarning = isWarning;
        notifyChanged();
    }

    /**
     * Set the action text
     */
    @VisibleForTesting
    public void setActionText(CharSequence actionText) {
        mActionText = actionText;
        notifyChanged();
    }

    @VisibleForTesting
    public ColorStateList getWarningTextColor() {
        return mWarningTextColor;
    }
}
