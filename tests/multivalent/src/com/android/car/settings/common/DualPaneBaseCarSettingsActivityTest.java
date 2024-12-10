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

package com.android.car.settings.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Intent;

import androidx.preference.Preference;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.car.settings.testutils.BaseCarSettingsTestActivity;
import com.android.car.settings.testutils.BaseTestSettingsFragment;
import com.android.car.settings.testutils.DualPaneTestActivity;

import org.junit.Rule;
import org.junit.Test;

public class DualPaneBaseCarSettingsActivityTest
        extends BaseCarSettingsActivityTestCase<DualPaneTestActivity> {

    @Rule
    public ActivityTestRule<DualPaneTestActivity> mActivityTestRule =
            new ActivityTestRule<>(DualPaneTestActivity.class);

    @Override
    ActivityTestRule<DualPaneTestActivity> getActivityTestRule() {
        return mActivityTestRule;
    }

    @Test
    public void onPreferenceStartFragment_startsActivity() throws Throwable {
        Preference pref = new Preference(mContext);
        pref.setFragment(BaseTestSettingsFragment.class.getName());

        BaseCarSettingsTestActivity spiedActivity = spy(mActivity);
        verify(spiedActivity, never()).startActivity(any(Intent.class));

        mActivityTestRule.runOnUiThread(() ->
                spiedActivity.onPreferenceStartFragment(/* caller= */ null, pref));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(spiedActivity).startActivity(any(Intent.class));
    }
}
