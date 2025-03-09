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

package com.android.car.settings.system;

import static android.app.Activity.RESULT_OK;

import static com.android.car.settings.common.SubSettingsActivity.KEY_SUB_SETTINGS_FRAGMENT;
import static com.android.car.settings.system.FactoryResetFragment.CHECK_LOCK_REQUEST_CODE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.car.settings.testutils.DualPaneTestActivity;

import org.junit.Rule;
import org.junit.Test;

public class DualPaneFactoryResetFragmentTest
        extends FactoryResetFragmentTestCase<DualPaneTestActivity> {

    @Rule
    public ActivityTestRule<DualPaneTestActivity> mActivityTestRule =
            new ActivityTestRule<>(DualPaneTestActivity.class);

    @Override
    ActivityTestRule<DualPaneTestActivity> getActivityTestRule() {
        return mActivityTestRule;
    }

    @Test
    public void processActivityResult_resultOk_launchesFactoryResetConfirmActivity()
            throws Throwable {
        getActivityTestRule().runOnUiThread(() -> {
            mFragment.processActivityResult(CHECK_LOCK_REQUEST_CODE, RESULT_OK, /* data= */ null);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        Intent intent = mActivity.getStartActivityListener().getLastValue();
        assertNotNull(intent);
        String fragmentClass = intent.getStringExtra(KEY_SUB_SETTINGS_FRAGMENT);
        assertNotNull(fragmentClass);
        assertThat(fragmentClass).isEqualTo(FactoryResetConfirmFragment.class.getName());
    }
}
