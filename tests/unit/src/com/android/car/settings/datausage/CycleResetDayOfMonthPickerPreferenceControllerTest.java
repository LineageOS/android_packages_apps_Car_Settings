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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.NetworkTemplate;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.BaseCarSettingsTestActivity;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.car.ui.preference.CarUiPreference;
import com.android.settingslib.NetworkPolicyEditor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CycleResetDayOfMonthPickerPreferenceControllerTest {

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private CarUxRestrictions mCarUxRestrictions;
    private CycleResetDayOfMonthPickerPreferenceController mPreferenceController;
    private Preference mPreference;

    @Mock
    private FragmentController mMockFragmentController;
    @Mock
    private NetworkPolicyEditor mMockPolicyEditor;
    @Mock
    private NetworkTemplate mMockNetworkTemplate;
    @Rule
    public ActivityTestRule<BaseCarSettingsTestActivity> mActivityTestRule =
            new ActivityTestRule<>(BaseCarSettingsTestActivity.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreferenceController = new CycleResetDayOfMonthPickerPreferenceController(mContext,
                /* preferenceKey= */ "key", mMockFragmentController,
                mCarUxRestrictions);
        mPreference = new CarUiPreference(mContext);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.setNetworkPolicyEditor(mMockPolicyEditor);
        mPreferenceController.setNetworkTemplate(mMockNetworkTemplate);
        mPreferenceController.onCreate(mLifecycleOwner);
    }

    @Test
    public void performClick_startsDialogWithStartingValue() throws Throwable {
        int startingValue = 15;
        when(mMockPolicyEditor.getPolicyCycleDay(mMockNetworkTemplate)).thenReturn(startingValue);
        mPreferenceController.refreshUi();
        mPreference.performClick();

        ArgumentCaptor<UsageCycleResetDayOfMonthPickerDialog> dialogCaptor =
                ArgumentCaptor.forClass(UsageCycleResetDayOfMonthPickerDialog.class);
        verify(mMockFragmentController).showDialog(
                dialogCaptor.capture(), anyString());

        UsageCycleResetDayOfMonthPickerDialog dialog = dialogCaptor.getValue();

        // Dialog was never started because FragmentController is mocked.
        mActivityTestRule.runOnUiThread(() -> {
            dialog.show(mActivityTestRule.getActivity().getSupportFragmentManager(), null);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertThat(dialog.getSelectedDayOfMonth()).isEqualTo(startingValue);
    }
}
