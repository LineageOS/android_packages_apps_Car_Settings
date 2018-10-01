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

package com.android.car.settings.common;

import static com.android.car.settings.common.BasePreferenceController.AVAILABLE;
import static com.android.car.settings.common.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.car.settings.common.BasePreferenceController.DISABLED_FOR_USER;
import static com.android.car.settings.common.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.testng.Assert.expectThrows;

import android.content.Context;

import com.android.car.settings.CarSettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/**
 * Unit test for {@link BasePreferenceController}.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class BasePreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    private FakePreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new FakePreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void createInstance() {
        BasePreferenceController controller = BasePreferenceController.createInstance(mContext,
                FakePreferenceController.class.getName(),
                PREFERENCE_KEY);

        assertThat(controller).isInstanceOf(FakePreferenceController.class);
        assertThat(controller.getPreferenceKey()).isEqualTo(PREFERENCE_KEY);
    }

    @Test
    public void keyMustBeSet() {
        expectThrows(IllegalArgumentException.class,
                () -> new FakePreferenceController(mContext, /* preferenceKey= */ null));
        expectThrows(IllegalArgumentException.class,
                () -> new FakePreferenceController(mContext, /* preferenceKey= */ ""));
    }

    @Test
    public void isAvailable_available_returnsTrue() {
        mController.setAvailabilityStatus(AVAILABLE);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_conditionallyUnavailable_returnsFalse() {
        mController.setAvailabilityStatus(CONDITIONALLY_UNAVAILABLE);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_unsupportedOnDevice_returnsFalse() {
        mController.setAvailabilityStatus(UNSUPPORTED_ON_DEVICE);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_disabledForUser_returnsFalse() {
        mController.setAvailabilityStatus(DISABLED_FOR_USER);

        assertThat(mController.isAvailable()).isFalse();
    }
}
