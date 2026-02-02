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
import static org.mockito.ArgumentMatchers.anyFloat;
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
import android.car.content.pm.CarPackageManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
public class CarDisplayDensityDialogHelperTest {
    private static final String TEST_PACKAGE_NAME = "com.test.package";
    private static final int TEST_USER_ID = 99;
    private static final int TEST_DISPLAY_ID = 20;
    private static final int SELECTED_RADIO_BUTTON_ID = 4;
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private Context mContext;
    @Mock
    private IActivityManager mActivityManager;
    @Mock
    private CarPackageManager mCarPackageManager;
    @Mock
    private RadioGroup mRadioGroup;
    @Mock
    private RadioButton mSelectedRadioButton;
    @Mock
    private Runnable mDismissRunnable;

    private CarDisplayDensityDialogHelper mHelper;
    private MockitoSession mSession;

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(com.android.systemui.car.Flags.FLAG_DISPLAY_COMPAT_V2);
        mSession = mockitoSession()
                .initMocks(this)
                .strictness(Strictness.LENIENT)
                .startMocking();
        ComponentName testComponentName = new ComponentName(TEST_PACKAGE_NAME, "TestClass");

        mHelper = new CarDisplayDensityDialogHelper(mContext, mActivityManager, mCarPackageManager,
                mDismissRunnable, testComponentName, TEST_USER_ID, TEST_DISPLAY_ID);
    }

    @After
    public void tearDown() {
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    public void submitResult_validArgs_setsDisplayDensityAndRestartsApp()
            throws RemoteException, PackageManager.NameNotFoundException {
        ArgumentCaptor<UserHandle> userHandleCaptor =
                ArgumentCaptor.forClass(UserHandle.class);
        float selectedDisplayDensity = 0.9f;

        mHelper.submitResult(mRadioGroup, String.valueOf(selectedDisplayDensity));

        verify(mCarPackageManager).setDensityScaleFactor(eq(TEST_PACKAGE_NAME), eq(TEST_USER_ID),
                eq(TEST_DISPLAY_ID), eq(selectedDisplayDensity));
        verify(mActivityManager).stopAppForUser(eq(TEST_PACKAGE_NAME), eq(TEST_USER_ID));
        verify(mContext).startActivityAsUser(nullable(Intent.class), userHandleCaptor.capture());
        assertThat(userHandleCaptor.getValue().getIdentifier()).isEqualTo(TEST_USER_ID);
        verify(mDismissRunnable, atLeastOnce()).run();
    }

    @Test
    public void submitResult_nonFloatSelectedTagValueArg_throwsIllegalStateException() {
        String nonFloatStringValue = "NON_FLOAT_STRING";

        assertThrows(IllegalStateException.class, () -> {
            mHelper.submitResult(mRadioGroup, nonFloatStringValue);
        });
    }

    @Test
    public void submitResult_nullSelectedTagValueArg_doesNothing()
            throws RemoteException, PackageManager.NameNotFoundException {
        mHelper.submitResult(mRadioGroup, /* selectedTagValue= */ null);

        verify(mCarPackageManager, never()).setDensityScaleFactor(anyString(), anyInt(), anyInt(),
                anyFloat());
        verify(mActivityManager, never()).stopAppForUser(anyString(), anyInt());
        verify(mContext, never()).startActivityAsUser(nullable(Intent.class),
                nullable(UserHandle.class));
        verify(mDismissRunnable, never()).run();
    }

    @Test
    public void setDefaultSelection_updateRadioGroup_selectsCorrectRadioButton()
            throws PackageManager.NameNotFoundException {
        float pkgDisplayDensity = 0.9f;
        when(mCarPackageManager.getDensityScaleFactor(eq(TEST_PACKAGE_NAME), eq(TEST_USER_ID),
                eq(TEST_DISPLAY_ID))).thenReturn(pkgDisplayDensity);
        when(mRadioGroup.getChildCount()).thenReturn(1);
        when(mRadioGroup.getChildAt(0)).thenReturn(mSelectedRadioButton);
        when(mSelectedRadioButton.getTag()).thenReturn(String.valueOf(pkgDisplayDensity));
        when(mSelectedRadioButton.getId()).thenReturn(SELECTED_RADIO_BUTTON_ID);

        mHelper.setDefaultSelection(mRadioGroup);

        verify(mRadioGroup).check(SELECTED_RADIO_BUTTON_ID);
    }
}
