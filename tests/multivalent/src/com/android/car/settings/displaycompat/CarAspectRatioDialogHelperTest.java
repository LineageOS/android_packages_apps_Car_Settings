/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.displaycompat;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockitoSession;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

@RunWith(AndroidJUnit4.class)
public class CarAspectRatioDialogHelperTest {
    private static final String TEST_PACKAGE_NAME = "com.test.package";
    private static final int TEST_USER_ID = 99;
    private static final int RADIO_BUTTON_FULLSCREEN_ID = 4;
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private Context mContext;
    @Mock
    private IPackageManager mPackageManager;
    @Mock
    private IActivityManager mActivityManager;
    @Mock
    private RadioGroup mRadioGroup;
    @Mock
    private RadioButton mFullScreenRadioButton;
    @Mock
    private Runnable mDismissRunnable;

    private CarAspectRatioDialogHelper mHelper;
    private MockitoSession mSession;

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(com.android.systemui.car.Flags.FLAG_DISPLAY_COMPAT_V2);
        mSession = mockitoSession()
                .initMocks(this)
                .strictness(Strictness.LENIENT)
                .startMocking();
        ComponentName testComponentName = new ComponentName(TEST_PACKAGE_NAME, "TestClass");

        mHelper = new CarAspectRatioDialogHelper(mContext, mPackageManager, mActivityManager,
                mDismissRunnable, testComponentName, TEST_USER_ID);
    }

    @After
    public void tearDown() {
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    public void submitResult_validArgs_setsAspectRatioAndRestartsApp() throws RemoteException {
        ArgumentCaptor<UserHandle> userHandleCaptor =
                ArgumentCaptor.forClass(UserHandle.class);

        mHelper.submitResult(mRadioGroup,
                String.valueOf(PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN));

        verify(mPackageManager).setUserMinAspectRatio(eq(TEST_PACKAGE_NAME), eq(TEST_USER_ID),
                eq(PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN));
        verify(mActivityManager).stopAppForUser(eq(TEST_PACKAGE_NAME), eq(TEST_USER_ID));
        verify(mContext).startActivityAsUser(nullable(Intent.class), userHandleCaptor.capture());
        assertThat(userHandleCaptor.getValue().getIdentifier()).isEqualTo(TEST_USER_ID);
        verify(mDismissRunnable, atLeastOnce()).run();
    }

    @Test
    public void submitResult_invalidSelectedTagValueArg_throwsIllegalStateException() {
        int incorrectMinAspectRatioValue = 999;

        assertThrows(IllegalStateException.class, () -> {
            mHelper.submitResult(mRadioGroup, String.valueOf(incorrectMinAspectRatioValue));
        });
    }

    @Test
    public void submitResult_nonIntegerSelectedTagValueArg_throwsIllegalStateException() {
        String nonIntegerStringValue = "NON_INTEGER_STRING";

        assertThrows(IllegalStateException.class, () -> {
            mHelper.submitResult(mRadioGroup, nonIntegerStringValue);
        });
    }

    @Test
    public void submitResult_nullSelectedTagValueArg_doesNothing() throws RemoteException {
        mHelper.submitResult(mRadioGroup, /* selectedTagValue= */ null);

        verify(mPackageManager, never()).setUserMinAspectRatio(anyString(), anyInt(), anyInt());
        verify(mActivityManager, never()).stopAppForUser(anyString(), anyInt());
        verify(mContext, never()).startActivityAsUser(nullable(Intent.class),
                nullable(UserHandle.class));
        verify(mDismissRunnable, never()).run();
    }

    @Test
    public void setDefaultSelection_updateRadioGroup_selectsCorrectRadioButton()
            throws RemoteException {
        when(mPackageManager.getUserMinAspectRatio(eq(TEST_PACKAGE_NAME), eq(TEST_USER_ID)))
                .thenReturn(PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN);
        when(mRadioGroup.getChildCount()).thenReturn(1);
        when(mRadioGroup.getChildAt(0)).thenReturn(mFullScreenRadioButton);
        when(mFullScreenRadioButton.getTag()).thenReturn(
                String.valueOf(PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN));
        when(mFullScreenRadioButton.getId()).thenReturn(RADIO_BUTTON_FULLSCREEN_ID);

        mHelper.setDefaultSelection(mRadioGroup);

        verify(mRadioGroup).check(RADIO_BUTTON_FULLSCREEN_ID);
    }
}
