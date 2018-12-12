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

import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.accounts.ConfirmAutoSyncChangeDialogFragment.OnConfirmListener;
import com.android.car.settings.testutils.ShadowContentResolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

/**
 * Tests for the {@link ConfirmAutoSyncChangeDialogFragment}.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowContentResolver.class})
public class ConfirmAutoSyncChangeDialogFragmentTest {
    private static final int USER_ID = 0;
    private static final UserHandle USER_HANDLE = new UserHandle(USER_ID);

    private TestDialogActivity mActivity;
    @Mock
    private OnConfirmListener mMockListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(TestDialogActivity.class);
    }

    @After
    public void tearDown() {
        ShadowContentResolver.reset();
    }

    @Test
    public void isEnabling_shouldSetCorrectTitle() {
        mActivity.showDialog(/* enabling= */ true, mMockListener);
        AlertDialog dialog = (AlertDialog) mActivity.getDialog().getDialog();

        assertThat(Shadows.shadowOf(dialog).getTitle()).isEqualTo(
                application.getString(R.string.data_usage_auto_sync_on_dialog_title));
    }

    @Test
    public void isDisabling_shouldSetCorrectTitle() {
        mActivity.showDialog(/* enabling= */ false, mMockListener);
        AlertDialog dialog = (AlertDialog) mActivity.getDialog().getDialog();

        assertThat(Shadows.shadowOf(dialog).getTitle()).isEqualTo(
                application.getString(R.string.data_usage_auto_sync_off_dialog_title));
    }

    @Test
    public void isEnabling_shouldSetCorrectMessage() {
        mActivity.showDialog(/* enabling= */ true, mMockListener);
        AlertDialog dialog = (AlertDialog) mActivity.getDialog().getDialog();

        assertThat(Shadows.shadowOf(dialog).getMessage()).isEqualTo(
                application.getString(R.string.data_usage_auto_sync_on_dialog));
    }

    @Test
    public void isDisabling_shouldSetCorrectMessage() {
        mActivity.showDialog(/* enabling= */ false, mMockListener);
        AlertDialog dialog = (AlertDialog) mActivity.getDialog().getDialog();

        assertThat(Shadows.shadowOf(dialog).getMessage()).isEqualTo(
                application.getString(R.string.data_usage_auto_sync_off_dialog));
    }

    @Test
    public void onButtonClick_isEnabling_shouldTurnOnMasterSyncAutomatically() {
        mActivity.showDialog(/* enabling= */ true, mMockListener);
        AlertDialog dialog = (AlertDialog) mActivity.getDialog().getDialog();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertThat(ContentResolver.getMasterSyncAutomaticallyAsUser(USER_ID)).isTrue();
    }

    @Test
    public void onButtonClick_isDisabling_shouldTurnOffMasterSyncAutomatically() {
        mActivity.showDialog(/* enabling= */ false, mMockListener);
        AlertDialog dialog = (AlertDialog) mActivity.getDialog().getDialog();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertThat(ContentResolver.getMasterSyncAutomaticallyAsUser(USER_ID)).isFalse();
    }

    @Test
    public void onButtonClick_isEnabling_shouldCallOnConfirmWithEnable() {
        mActivity.showDialog(/* enabling= */ true, mMockListener);
        AlertDialog dialog = (AlertDialog) mActivity.getDialog().getDialog();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        verify(mMockListener).onConfirm(true);
    }

    @Test
    public void onButtonClick_isDisabling_shouldCallOnConfirmWithDisable() {
        mActivity.showDialog(/* enabling= */ false, mMockListener);
        AlertDialog dialog = (AlertDialog) mActivity.getDialog().getDialog();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        verify(mMockListener).onConfirm(false);
    }

    public static class TestDialogActivity extends FragmentActivity {
        private static final String DIALOG_TAG = "dialog";

        private void showDialog(boolean enabling, OnConfirmListener listener) {
            ConfirmAutoSyncChangeDialogFragment dialogFragment =
                    ConfirmAutoSyncChangeDialogFragment.newInstance(enabling, USER_HANDLE,
                            listener);
            dialogFragment.show(getSupportFragmentManager(), DIALOG_TAG);
        }

        private ConfirmAutoSyncChangeDialogFragment getDialog() {
            return (ConfirmAutoSyncChangeDialogFragment) getSupportFragmentManager()
                    .findFragmentByTag(DIALOG_TAG);
        }
    }
}
