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

package com.android.car.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.car.ui.preference.CarUiPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AppDataUsageTotalPreferenceControllerTest {

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private CarUxRestrictions mCarUxRestrictions;
    private AppDataUsageTotalPreferenceController mPreferenceController;
    private CarUiPreference mPreference;

    @Mock
    private FragmentController mMockFragmentController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreferenceController = new AppDataUsageTotalPreferenceController(mContext,
                /* preferenceKey= */ "key", mMockFragmentController,
                mCarUxRestrictions);
        mPreference = new CarUiPreference(mContext);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void onCreate_setsTitle_usageIsZero() {
        long usage = 0;
        mPreferenceController.setDataUsage(usage);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                BidiFormatter.getInstance().unicodeWrap(
                        String.format(AppDataUsageTotalPreferenceController.STRING_FORMAT,
                                mContext.getString(R.string.data_usage_all_apps_title),
                                DataUsageUtils.bytesToIecUnits(mContext, usage).toString()),
                        TextDirectionHeuristics.LOCALE));
    }

    @Test
    public void onCreate_setsTitle_usageIsNotZero() {
        long usage = 10000000;
        mPreferenceController.setDataUsage(usage);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                BidiFormatter.getInstance().unicodeWrap(
                        String.format(AppDataUsageTotalPreferenceController.STRING_FORMAT,
                                mContext.getString(R.string.data_usage_all_apps_title),
                                DataUsageUtils.bytesToIecUnits(mContext, usage).toString()),
                        TextDirectionHeuristics.LOCALE));
    }
}
