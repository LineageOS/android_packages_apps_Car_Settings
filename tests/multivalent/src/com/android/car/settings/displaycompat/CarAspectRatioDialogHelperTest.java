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

import static android.os.UserHandle.USER_CURRENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
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
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CarAspectRatioDialogHelperTest {
    private static final String TEST_PACKAGE_NAME = "com.test.package";
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
    private View mDialogView;
    @Mock
    private Button mApplyButton;
    @Mock
    private Button mCloseButton;
    @Mock
    private RadioGroup mRadioGroup;
    @Mock
    private RadioButton mFullScreenRadioButton;

    private CarAspectRatioDialogHelper mHelper;
    private ComponentName mComponentName;
    private Runnable mDismissRunnable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSetFlagsRule.enableFlags(com.android.systemui.car.Flags.FLAG_DISPLAY_COMPATIBILITY_V2);
        mHelper = new CarAspectRatioDialogHelper(mContext, mPackageManager, mActivityManager);
        mComponentName = new ComponentName(TEST_PACKAGE_NAME, "TestClass");
        mDismissRunnable = mock(Runnable.class);
        setupMockViews();
    }

    private void setupMockViews() {
        when(mDialogView.findViewById(R.id.button_apply)).thenReturn(mApplyButton);
        when(mDialogView.findViewById(R.id.button_close)).thenReturn(mCloseButton);
        when(mDialogView.findViewById(R.id.aspect_ratio_radio_group)).thenReturn(mRadioGroup);
        when(mDialogView.findViewById(RADIO_BUTTON_FULLSCREEN_ID)).thenReturn(
                mFullScreenRadioButton);
    }

    @Test
    public void setupAspectRatioDialog_applyButton_isDisabledInitially() {
        when(mRadioGroup.getCheckedRadioButtonId()).thenReturn(-1);

        mHelper.setupAspectRatioDialog(mDialogView, mComponentName, USER_CURRENT, mDismissRunnable);

        verify(mApplyButton).setEnabled(false);
    }

    @Test
    public void setupAspectRatioDialog_applyButton_isEnabledAfterSelection() {
        ArgumentCaptor<RadioGroup.OnCheckedChangeListener> listenerCaptor =
                ArgumentCaptor.forClass(RadioGroup.OnCheckedChangeListener.class);
        ArgumentCaptor<Boolean> isEnabledCaptor = ArgumentCaptor.forClass(Boolean.class);

        mHelper.setupAspectRatioDialog(mDialogView, mComponentName, USER_CURRENT, mDismissRunnable);
        verify(mRadioGroup).setOnCheckedChangeListener(listenerCaptor.capture());
        listenerCaptor.getValue().onCheckedChanged(mRadioGroup, RADIO_BUTTON_FULLSCREEN_ID);

        verify(mApplyButton, atLeastOnce()).setEnabled(isEnabledCaptor.capture());
        assertThat(isEnabledCaptor.getAllValues().getLast()).isTrue();
    }

    @Test
    public void setupAspectRatioDialog_closeButton_dismissesDialog() {
        ArgumentCaptor<View.OnClickListener> listenerCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);

        mHelper.setupAspectRatioDialog(mDialogView, mComponentName, USER_CURRENT, mDismissRunnable);
        verify(mCloseButton).setOnClickListener(listenerCaptor.capture());
        listenerCaptor.getValue().onClick(mCloseButton);

        verify(mDismissRunnable).run();
    }

    @Test
    public void setupAspectRatioDialog_applyButton_setsAspectRatioAndRestartsApp()
            throws RemoteException {
        when(mRadioGroup.getCheckedRadioButtonId()).thenReturn(RADIO_BUTTON_FULLSCREEN_ID);
        when(mFullScreenRadioButton.getTag()).thenReturn(
                String.valueOf(PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN));
        ArgumentCaptor<View.OnClickListener> listenerCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);

        mHelper.setupAspectRatioDialog(mDialogView, mComponentName, USER_CURRENT, mDismissRunnable);
        verify(mApplyButton).setOnClickListener(listenerCaptor.capture());
        listenerCaptor.getValue().onClick(mApplyButton);

        verify(mPackageManager).setUserMinAspectRatio(eq(TEST_PACKAGE_NAME), eq(USER_CURRENT),
                eq(PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN));
        verify(mActivityManager).stopAppForUser(eq(TEST_PACKAGE_NAME), eq(USER_CURRENT));
        verify(mContext).startActivityAsUser(nullable(Intent.class),
                eq(UserHandle.of(USER_CURRENT)));
        verify(mDismissRunnable, atLeastOnce()).run();
    }

    @Test(expected = IllegalStateException.class)
    public void setupAspectRatioDialog_applyButton_invalidAspectRatio_throwsException() {
        when(mRadioGroup.getCheckedRadioButtonId()).thenReturn(RADIO_BUTTON_FULLSCREEN_ID);
        when(mFullScreenRadioButton.getTag()).thenReturn(String.valueOf(Integer.MAX_VALUE));
        ArgumentCaptor<View.OnClickListener> listenerCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);

        mHelper.setupAspectRatioDialog(mDialogView, mComponentName, USER_CURRENT, mDismissRunnable);
        verify(mApplyButton).setOnClickListener(listenerCaptor.capture());
        listenerCaptor.getValue().onClick(mApplyButton);

        // verify that error was thrown
    }

    @Test
    public void updateRadioGroup_selectsCorrectRadioButton() throws RemoteException {
        when(mPackageManager.getUserMinAspectRatio(eq(TEST_PACKAGE_NAME), eq(USER_CURRENT)))
                .thenReturn(PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN);
        when(mRadioGroup.getChildCount()).thenReturn(1);
        when(mRadioGroup.getChildAt(0)).thenReturn(mFullScreenRadioButton);
        when(mFullScreenRadioButton.getTag()).thenReturn(
                String.valueOf(PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN));
        when(mFullScreenRadioButton.getId()).thenReturn(RADIO_BUTTON_FULLSCREEN_ID);

        mHelper.setupAspectRatioDialog(mDialogView, mComponentName, USER_CURRENT, mDismissRunnable);

        verify(mRadioGroup).check(RADIO_BUTTON_FULLSCREEN_ID);
    }
}
