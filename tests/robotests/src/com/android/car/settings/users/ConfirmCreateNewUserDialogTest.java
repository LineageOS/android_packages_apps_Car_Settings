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
import com.android.car.settings.testutils.TestBaseFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

/**
 * Tests for ConfirmCreateNewDialog.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class ConfirmCreateNewUserDialogTest {
    private BaseTestActivity mTestActivity;
    private TestBaseFragment mTestFragment;
    private ConfirmCreateNewUserDialog mDialog;

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);

        mTestActivity = Robolectric.buildActivity(BaseTestActivity.class)
                .setup()
                .get();

        mTestFragment = new TestBaseFragment();
        mTestActivity.launchFragment(mTestFragment);

        mDialog = new ConfirmCreateNewUserDialog();
    }

    @Test
    public void testConfirmCreateNewUserInvokesOnCreateNewUserConfirmed() {
        ConfirmCreateNewUserDialog.ConfirmCreateNewUserListener listener = Mockito.mock(
                ConfirmCreateNewUserDialog.ConfirmCreateNewUserListener.class);
        mDialog.setConfirmCreateNewUserListener(listener);
        showDialog();

        // Invoke confirm create new user.
        DialogTestUtils.clickPositiveButton(mDialog);

        verify(listener).onCreateNewUserConfirmed();
        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testCancelInvokesOnCreateNewUserCancelledListener() {
        ConfirmCreateNewUserDialog.CancelCreateNewUserListener listener = Mockito.mock(
                ConfirmCreateNewUserDialog.CancelCreateNewUserListener.class);
        mDialog.setCancelCreateNewUserListener(listener);
        showDialog();

        // Invoke cancel.
        DialogTestUtils.clickNegativeButton(mDialog);

        verify(listener).onCreateNewUserCancelled();
        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testNoCancelClickListenerDismissesDialog() {
        showDialog();

        // Invoke cancel.
        DialogTestUtils.clickNegativeButton(mDialog);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    @Test
    public void testNoConfirmClickListenerDismissesDialog() {
        showDialog();

        // Invoke confirm add user.
        DialogTestUtils.clickPositiveButton(mDialog);

        assertThat(isDialogShown()).isFalse(); // Dialog is dismissed.
    }

    private void showDialog() {
        mDialog.show(mTestFragment);
        assertThat(isDialogShown()).isTrue();
    }

    private boolean isDialogShown() {
        return mTestActivity.getSupportFragmentManager()
                .findFragmentByTag(ConfirmCreateNewUserDialog.DIALOG_TAG) != null;
    }
}
