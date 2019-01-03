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

package com.android.car.settings.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.ActivityResultCallback;
import com.android.car.settings.common.PreferenceControllerTestHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class TrustedDeviceEntryPreferenceControllerTest {
    private Context mContext;
    private PreferenceControllerTestHelper<TrustedDeviceEntryPreferenceController>
            mPreferenceControllerHelper;
    private Preference mTrustedDevicePreference;
    private TrustedDeviceEntryPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mTrustedDevicePreference = new Preference(mContext);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                TrustedDeviceEntryPreferenceController.class, mTrustedDevicePreference);
        mController = mPreferenceControllerHelper.getController();
        mPreferenceControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @Test
    public void testHandlePreferenceClicked_listenerTriggered() {
        ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);
        mTrustedDevicePreference.performClick();
        verify(mPreferenceControllerHelper.getMockFragmentController()).startActivityForResult(
                intent.capture(), anyInt(), any(ActivityResultCallback.class));
        Assert.assertEquals(intent.getValue().getComponent().getClassName(),
                CheckLockActivity.class.getName());
    }

    @Test
    public void testProcessActivityResult_resultOk() {
        mController.processActivityResult(TrustedDeviceEntryPreferenceController.REQUEST_CODE,
                Activity.RESULT_OK, null);
        verify(mPreferenceControllerHelper.getMockFragmentController()).launchFragment(
                any(ChooseTrustedDeviceFragment.class));
    }

    @Test
    public void testProcessActivityResult_resultCanceled() {
        mController.processActivityResult(TrustedDeviceEntryPreferenceController.REQUEST_CODE,
                Activity.RESULT_CANCELED, null);
        verify(mPreferenceControllerHelper.getMockFragmentController(), never()).launchFragment(
                any(ChooseTrustedDeviceFragment.class));
    }
}
