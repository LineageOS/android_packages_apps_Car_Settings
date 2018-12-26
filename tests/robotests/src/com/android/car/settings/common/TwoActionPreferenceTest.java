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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class TwoActionPreferenceTest {

    private static class TestTwoActionPreference extends TwoActionPreference {

        TestTwoActionPreference(Context context) {
            super(context);
        }

        @Override
        protected void onBindWidgetFrame(View actionContainer) {
            // Intentionally empty.
        }
    }

    private PreferenceViewHolder mViewHolder;
    private TestTwoActionPreference mTestTwoActionPreference;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        Context themedContext = new ContextThemeWrapper(context, R.style.CarSettingTheme);
        View rootView = View.inflate(themedContext, R.layout.two_action_preference,
                null);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(rootView);
        mTestTwoActionPreference = new TestTwoActionPreference(context);
    }

    @Test
    public void showAction_true_buttonVisible() {
        mTestTwoActionPreference.showAction(true);
        mTestTwoActionPreference.onBindViewHolder(mViewHolder);

        assertThat(mViewHolder.findViewById(
                R.id.action_widget_container).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void showAction_false_buttonGone() {
        mTestTwoActionPreference.showAction(false);
        mTestTwoActionPreference.onBindViewHolder(mViewHolder);

        assertThat(mViewHolder.findViewById(
                R.id.action_widget_container).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void isActionShown_true() {
        mTestTwoActionPreference.showAction(true);
        assertThat(mTestTwoActionPreference.isActionShown()).isTrue();
    }

    @Test
    public void isActionShown_false() {
        mTestTwoActionPreference.showAction(false);
        assertThat(mTestTwoActionPreference.isActionShown()).isFalse();
    }
}
