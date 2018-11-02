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
package com.android.car.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.testutils.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLocationManager;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowSecureSettings.class,
        LocationControllerTest.CarShadowLocationManager.class})
public class LocationControllerTest {
    @Mock
    private LocationController.LocationChangeListener mListener;

    private Context mContext;
    private LocationController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new LocationController(mContext, mListener);
    }

    @Test
    public void onStart_registersBroadcastReceiver() {
        mController.onStart();
        mContext.sendBroadcast(new Intent(LocationManager.MODE_CHANGED_ACTION));
        verify(mListener).onLocationToggled(anyBoolean());
    }

    @Test
    public void onStop_shouldUnregisterListener() {
        mController.onStart();
        mContext.sendBroadcast(new Intent(LocationManager.MODE_CHANGED_ACTION));
        verify(mListener).onLocationToggled(anyBoolean());
        mController.onStop();
        mContext.sendBroadcast(new Intent(LocationManager.MODE_CHANGED_ACTION));

        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void setLocationEnabled_shouldDispatchOnLocationToggled() {
        mController.onStart();
        mController.setLocationEnabled(true);

        verify(mListener).onLocationToggled(/* enabled */ true);
    }

    @Test
    public void isEnabled_shouldReturnFalseWhenLocationIsOff() {
        mController.onStart();
        mController.setLocationEnabled(false);
        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isEnabled_shouldReturnTrueWhenLocationIsOn() {
        mController.onStart();
        mController.setLocationEnabled(true);
        assertThat(mController.isEnabled()).isTrue();
    }

    @Implements(value = LocationManager.class)
    public static class CarShadowLocationManager extends ShadowLocationManager {

        @Implementation
        public void setLocationEnabledForUser(boolean enabled, UserHandle userHandle) {
            ContentResolver cr = RuntimeEnvironment.application.getContentResolver();
            int newMode = enabled
                    ? Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
                    : Settings.Secure.LOCATION_MODE_OFF;

            Settings.Secure.putIntForUser(cr, Settings.Secure.LOCATION_MODE, newMode,
                    userHandle.getIdentifier());
            RuntimeEnvironment.application.sendBroadcast(new Intent(
                    LocationManager.MODE_CHANGED_ACTION));
        }
    }
}
