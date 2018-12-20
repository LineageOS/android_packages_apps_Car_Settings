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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.car.userlib.CarUserManagerHelper;
import android.content.ContentResolver;
import android.content.pm.UserInfo;

import androidx.lifecycle.Lifecycle;
import androidx.preference.SwitchPreference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowContentResolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

/** Unit tests for {@link AccountAutoSyncPreferenceController}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowContentResolver.class, ShadowCarUserManagerHelper.class})
public class AccountAutoSyncPreferenceControllerTest {
    private static final int USER_ID = 0;
    private PreferenceControllerTestHelper<AccountAutoSyncPreferenceController> mHelper;
    private SwitchPreference mSwitchPreference;
    private AccountAutoSyncPreferenceController mController;

    @Mock
    private CarUserManagerHelper mMockCarUserManagerHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mMockCarUserManagerHelper);
        doReturn(new UserInfo(USER_ID, "name", 0)).when(
                mMockCarUserManagerHelper).getCurrentProcessUserInfo();

        mSwitchPreference = new SwitchPreference(application);
        mHelper = new PreferenceControllerTestHelper<>(application,
                AccountAutoSyncPreferenceController.class, mSwitchPreference);
        mHelper.markState(Lifecycle.State.CREATED);
        mController = mHelper.getController();
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
    }

    @Test
    public void refreshUi_masterSyncOn_preferenceShouldBeChecked() {
        ContentResolver.setMasterSyncAutomaticallyAsUser(true, USER_ID);

        mController.refreshUi();

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void refreshUi_masterSyncOff_preferenceShouldNotBeChecked() {
        ContentResolver.setMasterSyncAutomaticallyAsUser(false, USER_ID);

        mController.refreshUi();

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceClicked_shouldOpenDialog() {
        mSwitchPreference.performClick();

        verify(mHelper.getMockFragmentController()).showDialog(
                any(ConfirmAutoSyncChangeDialogFragment.class), eq("confirmAutoSyncChange"));
    }

    @Test
    public void onConfirm_shouldTogglePreference() {
        // Set the preference as unchecked first so that the state is known
        ContentResolver.setMasterSyncAutomaticallyAsUser(false, USER_ID);
        mController.refreshUi();
        assertThat(mSwitchPreference.isChecked()).isFalse();

        mController.onConfirm(true);
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }
}
