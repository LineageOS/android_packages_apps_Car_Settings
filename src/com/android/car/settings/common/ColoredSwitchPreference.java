/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.R;
import com.android.car.ui.preference.CarUiSwitchPreference;
import com.android.settingslib.Utils;

/**
 * Extends {@link CarUiSwitchPreference} to customize colors for each state.
 */
public class ColoredSwitchPreference extends CarUiSwitchPreference {
    private int mEnabledColor;
    private int mDisabledColor;

    public ColoredSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public ColoredSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public ColoredSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColoredSwitchPreference(Context context) {
        super(context);
        init(context, /* attrs= */ null);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ColoredSwitchPreference);
        mDisabledColor = a.getColor(R.styleable.ColoredSwitchPreference_disabledTitleColor,
                Utils.getColorAttr(context, android.R.attr.textColorPrimary).getDefaultColor());
        mEnabledColor = a.getColor(R.styleable.ColoredSwitchPreference_enabledTitleColor,
                Utils.getColorAttr(context, android.R.attr.textColorPrimary).getDefaultColor());
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = holder.itemView.findViewById(android.R.id.title);
        if (title != null) {
            if (isChecked()) {
                title.setTextColor(mEnabledColor);
            } else {
                title.setTextColor(mDisabledColor);
            }
        }
    }
}
