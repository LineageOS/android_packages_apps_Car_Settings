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
import static org.robolectric.RuntimeEnvironment.application;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
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

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);

        mTestActivity = Robolectric.buildActivity(BaseTestActivity.class)
                .setup()
                .get();
    }

    @Test
    public void testConfirmRemoveUserTitle() {
        ConfirmRemoveUserDialog dlg =
                ConfirmRemoveUserDialog.create(ConfirmRemoveUserDialog.UserToRemove.ANY_USER);

        showDialog(dlg);

        assertThat(DialogTestUtils.getTitle(dlg))
                .isEqualTo(application.getString(R.string.delete_user_dialog_title));
    }

    @Test
    public void testConfirmRemoveLastUserTitle() {
        ConfirmRemoveUserDialog dlg =
                ConfirmRemoveUserDialog.create(ConfirmRemoveUserDialog.UserToRemove.LAST_USER);

        showDialog(dlg);

        assertThat(DialogTestUtils.getTitle(dlg))
                .isEqualTo(application.getString(R.string.delete_last_user_dialog_title));
    }

    @Test
    public void testConfirmAnyUserInvokesOnRemoveUserConfirmed() {
        ConfirmRemoveUserDialog dialog = ConfirmRemoveUserDialog.create(ConfirmRemoveUserDialog
                .UserToRemove.ANY_USER);
        ConfirmRemoveUserDialog.ConfirmRemoveUserListener listener = Mockito.mock(
                ConfirmRemoveUserDialog.ConfirmRemoveUserListener.class);
        dialog.setConfirmRemoveUserListener(listener);
        showDialog(dialog);

        // Invoke confirm remove user.
        DialogTestUtils.clickPositiveButton(dialog);

        verify(listener).onRemoveUserConfirmed();
        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testConfirmLastUserInvokesOnRemoveUserConfirmed() {
        ConfirmRemoveUserDialog dialog =
                ConfirmRemoveUserDialog.create(ConfirmRemoveUserDialog.UserToRemove.LAST_USER);
        ConfirmRemoveUserDialog.ConfirmRemoveUserListener listener = Mockito.mock(
                ConfirmRemoveUserDialog.ConfirmRemoveUserListener.class);
        dialog.setConfirmRemoveUserListener(listener);
        showDialog(dialog);

        // Invoke confirm remove user.
        DialogTestUtils.clickPositiveButton(dialog);

        verify(listener).onRemoveUserConfirmed();
        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testCancelOnRemoveAnyUserDialogDismissesDialog() {
        ConfirmRemoveUserDialog dlg =
                ConfirmRemoveUserDialog.create(ConfirmRemoveUserDialog.UserToRemove.ANY_USER);

        showDialog(dlg);

        assertThat(isDialogShown()).isTrue(); // Dialog is shown.

        // Invoke cancel.
        DialogTestUtils.clickNegativeButton(dlg);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testCancelOnRemoveLastUserDialogDismissesDialog() {
        ConfirmRemoveUserDialog dlg =
                ConfirmRemoveUserDialog.create(ConfirmRemoveUserDialog.UserToRemove.ANY_USER);

        showDialog(dlg);

        assertThat(isDialogShown()).isTrue(); // Dialog is shown.

        // Invoke cancel.
        DialogTestUtils.clickNegativeButton(dlg);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testNoClickListenerDismissesRemoveAnyUserDialog() {
        ConfirmRemoveUserDialog dlg =
                ConfirmRemoveUserDialog.create(ConfirmRemoveUserDialog.UserToRemove.ANY_USER);

        showDialog(dlg);

        // Invoke confirm remove user.
        DialogTestUtils.clickPositiveButton(dlg);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testNoClickListenerDismissesRemoveLastUserDialog() {
        ConfirmRemoveUserDialog dlg =
                ConfirmRemoveUserDialog.create(ConfirmRemoveUserDialog.UserToRemove.LAST_USER);

        showDialog(dlg);

        // Invoke confirm remove user.
        DialogTestUtils.clickPositiveButton(dlg);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    private void showDialog(ConfirmRemoveUserDialog dialog) {
        dialog.show(mTestActivity.getSupportFragmentManager(), CONFIRM_REMOVE_DIALOG_TAG);
    }

    private boolean isDialogShown() {
        return mTestActivity.getSupportFragmentManager()
                .findFragmentByTag(CONFIRM_REMOVE_DIALOG_TAG) != null;
    }
}
