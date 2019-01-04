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

package com.android.car.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.lifecycle.Lifecycle;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowTelephonyManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowTelephonyManager.class})
public class MobileDataTogglePreferenceControllerTest {

    private Context mContext;
    private TwoStatePreference mPreference;
    private PreferenceControllerTestHelper<MobileDataTogglePreferenceController>
            mControllerHelper;
    private MobileDataTogglePreferenceController mController;
    private TelephonyManager mTelephonyManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                MobileDataTogglePreferenceController.class, mPreference);
        mController = mControllerHelper.getController();
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);

        mTelephonyManager.setDataEnabled(false);
    }

    @After
    public void tearDown() {
        ShadowTelephonyManager.reset();
    }

    @Test
    public void refreshUi_dataEnabled_setChecked() {
        mTelephonyManager.setDataEnabled(true);
        mController.refreshUi();

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void refreshUi_dataDisabled_setUnchecked() {
        mTelephonyManager.setDataEnabled(false);
        mController.refreshUi();

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void handlePreferenceChanged_setFalse_opensDialog() {
        mPreference.callChangeListener(false);

        verify(mControllerHelper.getMockFragmentController()).showDialog(
                any(ConfirmMobileDataDisableDialog.class), eq(ConfirmMobileDataDisableDialog.TAG));
    }

    @Test
    public void handlePreferenceChanged_setTrue_enablesData() {
        mPreference.callChangeListener(true);

        assertThat(mTelephonyManager.isDataEnabled()).isTrue();
    }

    @Test
    public void handlePreferenceChanged_setTrue_checksSwitch() {
        mPreference.setChecked(false);
        mPreference.callChangeListener(true);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void onMobileDataDisableConfirmed_unchecksSwitch() {
        mTelephonyManager.setDataEnabled(true);
        mPreference.setChecked(true);

        mController.onMobileDataDisableConfirmed();

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onMobileDataDisableConfirmed_disablesMobileData() {
        mTelephonyManager.setDataEnabled(true);

        mController.onMobileDataDisableConfirmed();

        assertThat(mTelephonyManager.isDataEnabled()).isFalse();
    }

    @Test
    public void onMobileDataDisableRejected_checksSwitch() {
        mTelephonyManager.setDataEnabled(true);
        mPreference.setChecked(false);

        mController.onMobileDataDisableRejected();

        assertThat(mPreference.isChecked()).isTrue();
    }
}
