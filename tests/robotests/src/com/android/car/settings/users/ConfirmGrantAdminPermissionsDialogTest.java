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

package com.android.car.settings.users;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.pm.UserInfo;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.DialogTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

/**
 * Tests for ConfirmGrantAdminPermissionsDialog.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class ConfirmGrantAdminPermissionsDialogTest {

    private BaseTestActivity mTestActivity;

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);

        mTestActivity = Robolectric.setupActivity(BaseTestActivity.class);
    }

    @Test
    public void testConfirmGrantAdminInvokesOnGrantAdminConfirmed() {
        UserInfo testUser = new UserInfo();
        ConfirmGrantAdminPermissionsDialog dialog = new ConfirmGrantAdminPermissionsDialog();

        ConfirmGrantAdminPermissionsDialog.ConfirmGrantAdminListener listener =
                Mockito.mock(ConfirmGrantAdminPermissionsDialog.ConfirmGrantAdminListener.class);
        dialog.setUserToMakeAdmin(testUser);
        dialog.setConfirmGrantAdminListener(listener);
        showDialog(dialog);

        // Invoke confirm grant admin.
        DialogTestUtils.clickPositiveButton(dialog);

        verify(listener).onGrantAdminPermissionsConfirmed(testUser);
        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testCancelDismissesDialog() {
        ConfirmGrantAdminPermissionsDialog dialog = new ConfirmGrantAdminPermissionsDialog();
        showDialog(dialog);

        assertThat(isDialogShown()).isTrue(); // Dialog is shown.

        // Invoke cancel.
        DialogTestUtils.clickNegativeButton(dialog);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testNoClickListenerDismissesDialog() {
        ConfirmGrantAdminPermissionsDialog dialog = new ConfirmGrantAdminPermissionsDialog();
        showDialog(dialog);

        // Invoke confirm grant admin.
        DialogTestUtils.clickPositiveButton(dialog);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    private void showDialog(ConfirmGrantAdminPermissionsDialog dialog) {
        dialog.show(mTestActivity.getSupportFragmentManager(),
                ConfirmGrantAdminPermissionsDialog.TAG);
    }

    private boolean isDialogShown() {
        return mTestActivity.getSupportFragmentManager()
                .findFragmentByTag(ConfirmGrantAdminPermissionsDialog.TAG) != null;
    }
}
