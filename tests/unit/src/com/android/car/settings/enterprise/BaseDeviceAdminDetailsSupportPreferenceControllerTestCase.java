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
package com.android.car.settings.enterprise;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
abstract class BaseDeviceAdminDetailsSupportPreferenceControllerTestCase<T extends
        BaseDeviceAdminDetailsPreferenceController> {

    @Rule
    public final MockitoRule mockitorule = MockitoJUnit.rule();

    protected final String mPreferenceKey = "Da Key";
    protected final CarUxRestrictions mUxRestrictions = new CarUxRestrictions.Builder(
            /* reqOpt= */ true, CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED, /* time= */ 0)
                    .build();
    protected final Context mRealContext = ApplicationProvider.getApplicationContext();
    protected final Context mSpiedContext = spy(mRealContext);
    protected final ComponentName mAdmin =
            new ComponentName(mSpiedContext, TestDeviceAdminReceiver.class);

    protected DeviceAdminInfo mDeviceAdminInfo;

    @Mock
    protected FragmentController mFragmentController;
    @Mock
    protected DevicePolicyManager mDpm;
    @Mock
    protected PackageManager mPm;
    @Mock
    protected Preference mPreference;

    @Before
    public final void setFixtures() throws Exception {
        when(mSpiedContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDpm);
        when(mSpiedContext.getPackageManager()).thenReturn(mPm);

        ActivityInfo activityInfo = mRealContext.getPackageManager().getReceiverInfo(mAdmin,
                PackageManager.GET_META_DATA);

        mDeviceAdminInfo = new DeviceAdminInfo(mRealContext, activityInfo);
    }

    protected final void verifyPreferenceTitleNeverSet() {
        verify(mPreference, never()).setTitle(any());
    }

    protected final void verifyPreferenceTitleSet(CharSequence title) {
        verify(mPreference).setTitle(title);
    }
}
