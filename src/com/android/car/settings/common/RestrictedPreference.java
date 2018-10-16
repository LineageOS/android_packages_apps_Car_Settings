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

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;

/**
 * Preference class that supports being disabled by a user restriction set by a device admin.
 *
 * <p>This is a version of com.android.settingslib.RestrictedPreference which permits applying
 * car specific styles.
 */
public class RestrictedPreference extends Preference {
    RestrictedPreferenceHelper mHelper;

    public RestrictedPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public RestrictedPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        init(context, attrs);
    }

    public RestrictedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RestrictedPreference(Context context) {
        this(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        mHelper = new RestrictedPreferenceHelper(context, this, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mHelper.onBindViewHolder(holder);
    }

    @Override
    public void performClick() {
        if (!mHelper.performClick()) {
            super.performClick();
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        // Allow the helper to modify the state of this preference before the super implementation.
        mHelper.onAttachedToHierarchy();
        super.onAttachedToHierarchy(preferenceManager);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && isDisabledByAdmin()) {
            mHelper.setDisabledByAdmin(null);
            // Helper will handle modifying enabled state internally.
            return;
        }
        super.setEnabled(enabled);
    }

    /** Disables this preference with the restrictions enforced by the given admin. */
    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        if (mHelper.setDisabledByAdmin(admin)) {
            notifyChanged();
        }
    }

    /** Returns {@code true} if the preference is currently disabled by an admin user. */
    public boolean isDisabledByAdmin() {
        return mHelper.isDisabledByAdmin();
    }
}
