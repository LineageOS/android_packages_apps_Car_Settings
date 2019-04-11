/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.text.InputType;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.R;

/**
 * Extends {@link EditTextPreference} for entering password input. When {@link SettingsFragment}
 * detects an instance of this class, it creates a new instance of {@link
 * SettingsEditTextPreferenceDialogFragment} with the inputType parameter set to
 * InputType.TYPE_TEXT_VARIATION_PASSWORD, so that the input is obscured on the dialog's TextEdit.
 * OnPreferenceChange, it either obscures the raw password input to display as the preference's
 * summary or displays the default password summary if the input is empty.
 */
public class PasswordEditTextPreference extends EditTextPreference {

    private OnPreferenceChangeListener mUserProvidedListener;
    private TextView mSummaryView;
    private final OnPreferenceChangeListener mCombinedListener = (preference, newValue) -> {
        if (mUserProvidedListener != null) {
            mUserProvidedListener.onPreferenceChange(preference, newValue);
        }
        obscurePasswordForPreferenceSummary(preference, newValue);
        return true;
    };

    public PasswordEditTextPreference(Context context) {
        super(context);
        super.setOnPreferenceChangeListener(mCombinedListener);
        setSummary(R.string.default_password_summary);
        setPersistent(false);
    }

    @Override
    public void setOnPreferenceChangeListener(OnPreferenceChangeListener userProvidedListener) {
        mUserProvidedListener = userProvidedListener;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mSummaryView = (TextView) holder.findViewById(android.R.id.summary);
    }

    @VisibleForTesting
    int getSummaryInputType() {
        return mSummaryView.getInputType();
    }

    private void obscurePasswordForPreferenceSummary(Preference preference, Object password) {
        CharSequence value = password.toString();
        if (TextUtils.isEmpty(value)) {
            value = getContext().getString(R.string.default_password_summary);
            mSummaryView.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            mSummaryView.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        preference.setSummary(value);
    }
}
