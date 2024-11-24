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

package com.android.car.settings.bluetooth;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageManager;
import android.uwb.UwbManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class UwbTogglePreferenceControllerTest {
    private Context mContext = spy(ApplicationProvider.getApplicationContext());
    private SwitchPreference mPreference;
    private LifecycleOwner mLifecycleOwner;
    private UwbTogglePreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private UwbManager mMockUwbManager;
    @Mock
    private PackageManager mMockPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        when(mContext.getSystemService(UwbManager.class)).thenReturn(mMockUwbManager);
        when(mContext.getPackageManager()).thenReturn(mMockPackageManager);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        mPreferenceController = new UwbTogglePreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions);
        mPreference = new SwitchPreference(mContext);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void getDefaultAvailabilityStatus_noAvailableOnDevice() {
        when(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_UWB)).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testDefaultAvailabilityStatus_availableOnDevice() {
        when(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_UWB)).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    public void testUpdateState_stateChecked() {
        when(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_UWB)).thenReturn(true);
        when(mMockUwbManager.isUwbEnabled()).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_stateNotChecked() {
        when(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_UWB)).thenReturn(true);
        when(mMockUwbManager.isUwbEnabled()).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testHandlePreferenceChanged_enableUWB() {
        when(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_UWB)).thenReturn(true);
        when(mMockUwbManager.isUwbEnabled()).thenReturn(false);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        mPreferenceController.handlePreferenceChanged(mPreference, true);

        verify(mMockUwbManager).setUwbEnabled(true);
    }
}
