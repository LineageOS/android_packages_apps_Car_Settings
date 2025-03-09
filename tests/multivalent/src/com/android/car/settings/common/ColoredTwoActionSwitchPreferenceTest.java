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

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ColoredTwoActionSwitchPreferenceTest {
    private static final String ACTION_TEXT = "action";
    private Context mContext = ApplicationProvider.getApplicationContext();
    private View mRootView;
    private ColoredTwoActionSwitchPreference mPref;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        mRootView = View.inflate(mContext,
                R.layout.colored_two_action_switch_preference, /* parent= */ null);
        mHolder = PreferenceViewHolder.createInstanceForTests(mRootView);
        mPref = new ColoredTwoActionSwitchPreference(mContext);
    }

    @Test
    public void onBindViewHolder_noSetActionText_shouldNotBeVisible() {
        mPref.onBindViewHolder(mHolder);

        assertThat(mRootView.findViewById(R.id.action_text).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void onBindViewHolder_setActionText_shouldBeVisible() {
        mPref.setActionText(ACTION_TEXT);

        mPref.onBindViewHolder(mHolder);

        assertThat(mRootView.findViewById(R.id.action_text).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(((TextView) mRootView.findViewById(R.id.action_text)).getText())
                .isEqualTo(ACTION_TEXT);
    }

    @Test
    public void setSecondaryActionChecked_setToTrue_switchSetToTrue() {
        mPref.setSecondaryActionChecked(true);

        mPref.onBindViewHolder(mHolder);

        assertTrue(((Switch) mRootView.findViewById(
                R.id.colored_preference_secondary_action_concrete)).isChecked());
    }

    @Test
    public void setIsWarning_setToTrue_yellowActionText() {
        mPref.setIsWarning(true);
        mPref.setActionText(ACTION_TEXT);

        mPref.onBindViewHolder(mHolder);

        assertThat(((TextView) mRootView.findViewById(R.id.action_text)).getText())
                .isEqualTo(ACTION_TEXT);
        assertThat(((TextView) mRootView.findViewById(R.id.action_text)).getTextColors())
                .isEqualTo(mPref.getWarningTextColor());
    }
}
