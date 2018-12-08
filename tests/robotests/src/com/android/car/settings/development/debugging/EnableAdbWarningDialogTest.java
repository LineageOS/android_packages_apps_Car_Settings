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

package com.android.car.settings.development.debugging;

import static org.mockito.Mockito.verify;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.testutils.BaseTestActivity;
import com.android.car.settings.testutils.DialogTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class EnableAdbWarningDialogTest {
    private BaseTestActivity mTestActivity;
    private EnableAdbWarningDialog mDialog;
    @Mock
    private EnableAdbWarningDialog.AdbToggleListener mListener;

    @Before
    public void setUpTestActivity() {
        MockitoAnnotations.initMocks(this);

        mTestActivity = Robolectric.setupActivity(BaseTestActivity.class);
        mDialog = new EnableAdbWarningDialog();
        mDialog.setAdbToggleListener(mListener);
        mTestActivity.showDialog(mDialog, EnableAdbWarningDialog.TAG);
    }

    @Test
    public void testPositiveButton_confirmListenerFired() {
        DialogTestUtils.clickPositiveButton(mDialog);
        verify(mListener).onAdbEnableConfirmed();
    }

    @Test
    public void testNegativeButton_rejectListenerFired() {
        DialogTestUtils.clickNegativeButton(mDialog);
        verify(mListener).onAdbEnableRejected();
    }
}
