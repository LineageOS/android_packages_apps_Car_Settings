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
 * Tests for ConfirmRemoveUserDialog.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class ConfirmRemoveUserDialogTest {
    private static final String CONFIRM_REMOVE_DIALOG_TAG = "ConfirmRemoveUserDialog";
    private BaseTestActivity mTestActivity;
    private ConfirmRemoveUserDialog mConfirmRemoveUserDialog;

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);

        mTestActivity = Robolectric.buildActivity(BaseTestActivity.class)
                .setup()
                .get();

        mConfirmRemoveUserDialog = new ConfirmRemoveUserDialog();
    }

    @Test
    public void testConfirmRemoveUserInvokesOnRemoveUserConfirmed() {
        ConfirmRemoveUserDialog.ConfirmRemoveUserListener listener = Mockito.mock(
                ConfirmRemoveUserDialog.ConfirmRemoveUserListener.class);
        mConfirmRemoveUserDialog.setConfirmRemoveUserListener(listener);
        showDialog();

        // Invoke confirm remove user.
        DialogTestUtils.clickPositiveButton(mConfirmRemoveUserDialog);

        verify(listener).onRemoveUserConfirmed();
        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testCancelDismissesDialog() {
        showDialog();

        assertThat(isDialogShown()).isTrue(); // Dialog is shown.

        // Invoke cancel.
        DialogTestUtils.clickNegativeButton(mConfirmRemoveUserDialog);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testNoClickListenerDismissesDialog() {
        showDialog();

        // Invoke confirm remove user.
        DialogTestUtils.clickPositiveButton(mConfirmRemoveUserDialog);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    private void showDialog() {
        mConfirmRemoveUserDialog.show(
                mTestActivity.getSupportFragmentManager(), CONFIRM_REMOVE_DIALOG_TAG);
    }

    private boolean isDialogShown() {
        return mTestActivity.getSupportFragmentManager()
                .findFragmentByTag(CONFIRM_REMOVE_DIALOG_TAG) != null;
    }
}
