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

package com.android.car.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.ui.preference.CarUiSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OemNetworkPreferenceControllerTest extends MobileNetworkTestCase {

    private CarUiSwitchPreference mPreference;
    private OemNetworkPreferenceController mPreferenceController;

    @Before
    @UiThreadTest
    public void setUp() {
        super.setUp();

        mPreference = new CarUiSwitchPreference(mContext);
        mPreferenceController = new OemNetworkPreferenceController(mContext,
                "key", mFragmentController, mCarUxRestrictions);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @Test
    public void onStart_noNetworks_notVisible() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void onStart_oneOemNetwork_visibleAndSummarySet() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(TEST_DISPLAY_NAME);
    }

    @Test
    public void onToggleOff_showsDialog() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        mPreferenceController.mNetworkCallback.onAvailable(mNetwork);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(TEST_DISPLAY_NAME);

        mPreference.callChangeListener(false);

        verify(mFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(ConfirmationDialogFragment.TAG));
    }
}
