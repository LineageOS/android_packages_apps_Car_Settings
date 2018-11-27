/*
 * Copyright 2018 The Android Open Source Project
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
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.R;

/**
 * {@link Preference} with a secondary clickable button on the side.
 * {@link #setLayoutResource(int)} or the {@code widgetLayout} resource may be used to specify
 * the icon to display in the button. The button is shown by default.
 * {@link #showButton(boolean)} may be used to manually set the visibility of the button.
 *
 * <p>Note: the button is enabled even when {@link #isEnabled()} is {@code false}.
 */
public class ButtonPreference extends Preference {

    /**
     * Interface definition for a callback to be invoked when the button is clicked.
     */
    public interface OnButtonClickListener {
        /**
         * Called when a button has been clicked.
         *
         * @param preference the preference whose button was clicked.
         */
        void onButtonClick(ButtonPreference preference);
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnButtonClickListener != null) {
                mOnButtonClickListener.onButtonClick(ButtonPreference.this);
            }
        }
    };

    private OnButtonClickListener mOnButtonClickListener;
    private boolean mIsButtonShown = true;

    public ButtonPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ButtonPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.button_preference);
    }

    /**
     * Sets an {@link OnButtonClickListener} to be invoked when the button is clicked.
     */
    public void setOnButtonClickListener(OnButtonClickListener listener) {
        mOnButtonClickListener = listener;
    }

    /**
     * Sets whether the secondary button is visible in the preference.
     *
     * @param isShown {@code true} if the button should be shown.
     */
    public void showButton(boolean isShown) {
        mIsButtonShown = isShown;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View button = holder.findViewById(R.id.button_preference_button);
        if (mIsButtonShown) {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(mOnClickListener);
            button.setEnabled(true);  // Available even if the preference is disabled.
        } else {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
        }
    }
}
