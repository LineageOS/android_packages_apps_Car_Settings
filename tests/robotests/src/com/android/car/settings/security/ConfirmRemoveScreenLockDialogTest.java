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

package com.android.car.settings.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;

import androidx.car.app.CarAlertDialog;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowLockPatternUtils;
import com.android.internal.widget.LockPatternUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {
        ShadowCarUserManagerHelper.class,
        ShadowLockPatternUtils.class
})
public class ConfirmRemoveScreenLockDialogTest {

    private static final String TEST_CURRENT_PASSWORD = "test_password";
    private static final int TEST_USER = 10;
    private Context mContext;
    private ConfirmRemoveScreenLockDialog mDialogFragment;
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private LockPatternUtils mLockPatternUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        ShadowLockPatternUtils.setInstance(mLockPatternUtils);
        when(mCarUserManagerHelper.getCurrentProcessUserId()).thenReturn(TEST_USER);
        mContext = RuntimeEnvironment.application;
        mDialogFragment = new ConfirmRemoveScreenLockDialog(mContext, mFragmentController,
                TEST_CURRENT_PASSWORD);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
        ShadowLockPatternUtils.reset();
    }

    @Test
    public void testOnClick_goBack() {
        mDialogFragment.onClick(new CarAlertDialog.Builder(mContext).create(), 0);
        verify(mFragmentController).goBack();
    }

    @Test
    public void testOnClick_clearLock() {
        mDialogFragment.onClick(new CarAlertDialog.Builder(mContext).create(), 0);
        verify(mLockPatternUtils).clearLock(TEST_CURRENT_PASSWORD, TEST_USER);
    }
}
