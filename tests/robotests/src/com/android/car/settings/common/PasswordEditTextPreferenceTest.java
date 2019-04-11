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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.text.InputType;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class PasswordEditTextPreferenceTest {

    private Context mContext;
    private PasswordEditTextPreference mPreference;
    private PreferenceViewHolder mViewHolder;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        View rootView = View.inflate(mContext, R.layout.preference, /* root= */ null);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(rootView);
        mPreference = new PasswordEditTextPreference(mContext);
    }

    @Test
    public void onPasswordEntryEmpty_shouldShowDefaultPreferenceSummary() {
        mPreference.onBindViewHolder(mViewHolder);
        mPreference.callChangeListener("");

        assertThat(mPreference.getSummaryInputType()).isEqualTo(InputType.TYPE_CLASS_TEXT);
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.default_password_summary));
    }

    @Test
    public void onPasswordEntryNotEmpty_shouldShowObscuredPasswordPreferenceSummary() {
        mPreference.onBindViewHolder(mViewHolder);
        String testPassword = "TEST_PASSWORD";
        mPreference.callChangeListener(testPassword);

        assertThat(mPreference.getSummaryInputType()).isEqualTo((InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        assertThat(mPreference.getSummary()).isEqualTo(testPassword);
    }
}
