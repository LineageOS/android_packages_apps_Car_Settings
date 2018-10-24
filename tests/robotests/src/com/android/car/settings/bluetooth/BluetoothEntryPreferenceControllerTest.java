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

package com.android.car.settings.bluetooth;

import static com.android.car.settings.common.BasePreferenceController.AVAILABLE;
import static com.android.car.settings.common.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.lang.reflect.Field;

/** Unit test for {@link BluetoothEntryPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class BluetoothEntryPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    private BluetoothEntryPreferenceController mController;

    @Before
    public void setUp() {
        mController = new BluetoothEntryPreferenceController(RuntimeEnvironment.application,
                PREFERENCE_KEY, mock(FragmentController.class));
    }

    @Test
    public void getAvailabilityStatus_defaultAdapterAvailable_available() {
        // Bluetooth adapter is always available in Robolectric.
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_defaultAdapterNull_unsupportedOnDevice()
            throws NoSuchFieldException, IllegalAccessException {
        // Since Robolectric doesn't allow setting the adapter availability, we null it here.
        // See BluetoothAdapter.getDefaultAdapter Javadoc for API behavior.
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        Field defaultAdapterField = shadowApplication.getClass().getDeclaredField(
                "bluetoothAdapter");
        defaultAdapterField.setAccessible(true);
        defaultAdapterField.set(shadowApplication, null);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
