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

package com.android.car.settings.location;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.CONDITIONALLY_UNAVAILABLE;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.location.LocationManager;
import android.os.PackageTagsList;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.Flags;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class LocationAccessPreferenceControllerTest {
    private Context mContext = spy(ApplicationProvider.getApplicationContext());
    private CarUxRestrictions mCarUxRestrictions;
    private LocationAccessPreferenceController mController;

    @Mock
    private LocationManager mLocationManager;
    @Mock
    private FragmentController mFragmentController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemService(LocationManager.class)).thenReturn(mLocationManager);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mController = new LocationAccessPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions);
    }

    @Test
    @DisableFlags(Flags.FLAG_REQUIRED_INFOTAINMENT_APPS_SETTINGS_PAGE)
    public void getAvailabilityStatus_adasAllowlistNotEmpty_available() {
        PackageTagsList list = new PackageTagsList.Builder().add("testApp1").build();
        when(mLocationManager.getAdasAllowlist()).thenReturn(list);

        PreferenceControllerTestUtil.assertAvailability(mController.getAvailabilityStatus(),
                AVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_REQUIRED_INFOTAINMENT_APPS_SETTINGS_PAGE)
    public void getAvailabilityStatus_adasAllowlistNotEmpty_conditionallyUnavailable_zoneHidden() {
        mController.setAvailabilityStatusForZone("hidden");
        PackageTagsList list = new PackageTagsList.Builder().add("testApp1").build();
        when(mLocationManager.getAdasAllowlist()).thenReturn(list);

        PreferenceControllerTestUtil.assertAvailability(mController.getAvailabilityStatus(),
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_REQUIRED_INFOTAINMENT_APPS_SETTINGS_PAGE)
    public void getAvailabilityStatus_adasAllowlistEmpty_conditionallyUnavailable() {
        PackageTagsList list = new PackageTagsList.Builder().build();
        when(mLocationManager.getAdasAllowlist()).thenReturn(list);

        PreferenceControllerTestUtil.assertAvailability(mController.getAvailabilityStatus(),
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_REQUIRED_INFOTAINMENT_APPS_SETTINGS_PAGE)
    public void getAvailabilityStatus_adasAllowlistEmpty_conditionallyUnavailable_zoneHidden() {
        mController.setAvailabilityStatusForZone("hidden");
        PackageTagsList list = new PackageTagsList.Builder().build();
        when(mLocationManager.getAdasAllowlist()).thenReturn(list);

        PreferenceControllerTestUtil.assertAvailability(mController.getAvailabilityStatus(),
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_REQUIRED_INFOTAINMENT_APPS_SETTINGS_PAGE)
    public void getAvailabilityStatus_available() {
        PreferenceControllerTestUtil.assertAvailability(mController.getAvailabilityStatus(),
                AVAILABLE);
    }
}
