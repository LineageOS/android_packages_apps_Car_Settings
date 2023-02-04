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

import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.car.ui.preference.CarUiPreference;
import com.android.settingslib.net.DataUsageController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class DataWarningAndLimitPreferenceControllerTest {

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private CarUxRestrictions mCarUxRestrictions;
    private DataWarningAndLimitPreferenceController mPreferenceController;
    private CarUiPreference mPreference;

    @Mock
    private FragmentController mMockFragmentController;
    @Mock
    private DataUsageController mMockDataUsageController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreferenceController = new DataWarningAndLimitPreferenceController(mContext,
                /* preferenceKey= */ "key", mMockFragmentController,
                mCarUxRestrictions, mMockDataUsageController);
        mPreference = new CarUiPreference(mContext);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void onCreate_testSummary_hasWarningAndLimitLevel() {
        long warningLevel = 1000000;
        long limitLevel = 2000000;
        initDataUsageInfo(warningLevel, limitLevel);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                TextUtils.expandTemplate(
                        mContext.getText(R.string.cell_data_warning_and_limit),
                        DataUsageUtils.bytesToIecUnits(mContext, warningLevel),
                        DataUsageUtils.bytesToIecUnits(mContext, limitLevel).toString()));
    }

    @Test
    public void onCreate_testSummary_hasWarningLevel() {
        long warningLevel = 1000000;
        long limitLevel = 0;
        initDataUsageInfo(warningLevel, limitLevel);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                TextUtils.expandTemplate(mContext.getText(R.string.cell_data_warning),
                        DataUsageUtils.bytesToIecUnits(mContext, warningLevel).toString()));
    }

    @Test
    public void onCreate_testSummary_hasLimitLevel() {
        long warningLevel = 0;
        long limitLevel = 2000000;
        initDataUsageInfo(warningLevel, limitLevel);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                TextUtils.expandTemplate(mContext.getText(R.string.cell_data_limit),
                        DataUsageUtils.bytesToIecUnits(mContext, limitLevel).toString()));
    }

    private void initDataUsageInfo(long warningLevel, long limitLevel) {
        DataUsageController.DataUsageInfo dataUsageInfo = new DataUsageController.DataUsageInfo();
        dataUsageInfo.warningLevel = warningLevel;
        dataUsageInfo.limitLevel = limitLevel;
        when(mMockDataUsageController.getDataUsageInfo(mPreferenceController.getNetworkTemplate()))
                .thenReturn(dataUsageInfo);
        mPreferenceController.onCreate(mLifecycleOwner);
    }
}
