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

package com.android.car.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.robolectric.RuntimeEnvironment.application;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Unit test for {@link AccountDetailsTitlePreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class AccountDetailsTitlePreferenceControllerTest {
    private static final String PREFERENCE_KEY = "preference_key";

    private AccountDetailsTitlePreferenceController mController;

    @Before
    public void setUp() {
        mController = new AccountDetailsTitlePreferenceController(RuntimeEnvironment.application,
                PREFERENCE_KEY, mock(FragmentController.class));
    }

    @Test
    public void displayPreferences_shouldSetTitle() {
        mController.setTitle("Title");

        PreferenceScreen screen = new PreferenceManager(application).createPreferenceScreen(
                application);
        mController.displayPreference(screen);

        assertThat(screen.getTitle().toString()).isEqualTo("Title");
    }
}
