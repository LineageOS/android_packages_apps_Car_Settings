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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.common.ButtonPreference.OnButtonClickListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class ButtonPreferenceTest {

    private PreferenceViewHolder mViewHolder;
    private ButtonPreference mButtonPreference;

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        final Context themedContext = new ContextThemeWrapper(context, R.style.CarSettingTheme);

        View rootView = View.inflate(themedContext, R.layout.button_preference, null);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(rootView);
        mButtonPreference = new ButtonPreference(context);
    }

    @Test
    public void buttonClicked_callsListener() {
        mButtonPreference.onBindViewHolder(mViewHolder);
        OnButtonClickListener listener = mock(OnButtonClickListener.class);
        mButtonPreference.setOnButtonClickListener(listener);

        mViewHolder.findViewById(R.id.button_preference_button).performClick();

        verify(listener).onButtonClick(mButtonPreference);
    }

    @Test
    public void showButton_true_buttonVisible() {
        mButtonPreference.showButton(true);
        mButtonPreference.onBindViewHolder(mViewHolder);

        assertThat(
                mViewHolder.findViewById(R.id.button_preference_button).getVisibility()).isEqualTo(
                View.VISIBLE);
    }

    @Test
    public void showButton_false_buttonGone() {
        mButtonPreference.showButton(false);
        mButtonPreference.onBindViewHolder(mViewHolder);

        assertThat(
                mViewHolder.findViewById(R.id.button_preference_button).getVisibility()).isEqualTo(
                View.GONE);
    }
}
