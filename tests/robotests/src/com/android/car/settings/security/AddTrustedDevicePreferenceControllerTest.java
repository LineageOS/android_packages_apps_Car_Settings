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

package com.android.car.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowLockPatternUtils;
import com.android.internal.widget.LockPatternUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;


/**
 * Unit tests for {@link AddTrustedDevicePreferenceController}.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowLockPatternUtils.class})
public class AddTrustedDevicePreferenceControllerTest {
    private Context mContext;
    private PreferenceControllerTestHelper<AddTrustedDevicePreferenceController>
            mPreferenceControllerHelper;
    @Mock
    private LockPatternUtils mLockPatternUtils;
    private Preference mPreference;
    private AddTrustedDevicePreferenceController mController;
    private CarUserManagerHelper mCarUserManagerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ShadowLockPatternUtils.setInstance(mLockPatternUtils);
        mPreference = new Preference(mContext);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                AddTrustedDevicePreferenceController.class, mPreference);
        mController = mPreferenceControllerHelper.getController();
        mCarUserManagerHelper = new CarUserManagerHelper(mContext);
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @After
    public void tearDown() {
        ShadowLockPatternUtils.reset();
    }

    @Test
    public void refreshUi_hasPassword_preferenceEnabled() {
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(
                mCarUserManagerHelper.getCurrentProcessUserId())).thenReturn(
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        mController.refreshUi();

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void refreshUi_noPassword_preferenceDisabled() {
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt())).thenReturn(
                DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        mController.refreshUi();

        assertThat(mPreference.isEnabled()).isFalse();
    }
}
