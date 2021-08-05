/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.fragment.app.FragmentManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.car.settings.testutils.BaseCarSettingsTestActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class UsageCycleResetDayOfMonthPickerDialogTest {

    private FragmentManager mFragmentManager;

    @Mock
    private UsageCycleResetDayOfMonthPickerDialog.ResetDayOfMonthPickedListener
            mResetDayOfMonthPickedListener;
    @Rule
    public ActivityTestRule<BaseCarSettingsTestActivity> mActivityTestRule =
            new ActivityTestRule<>(BaseCarSettingsTestActivity.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragmentManager = mActivityTestRule.getActivity().getSupportFragmentManager();
    }

    @Test
    public void dialogInit_validValue_showsCurrentValue() throws Throwable {
        int setDate = 15;
        UsageCycleResetDayOfMonthPickerDialog dialog =
                UsageCycleResetDayOfMonthPickerDialog.newInstance(setDate);
        mActivityTestRule.runOnUiThread(() -> {
            dialog.show(mFragmentManager, /* tag= */ null);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(dialog.getSelectedDayOfMonth()).isEqualTo(setDate);
    }

    @Test
    public void dialogInit_lowInvalidValue_showsLowestPossibleValue() throws Throwable {
        UsageCycleResetDayOfMonthPickerDialog dialog =
                UsageCycleResetDayOfMonthPickerDialog.newInstance(0);
        mActivityTestRule.runOnUiThread(() -> {
            dialog.show(mFragmentManager, /* tag= */ null);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(dialog.getSelectedDayOfMonth()).isEqualTo(1);
    }

    @Test
    public void dialogInit_highInvalidValue_showsHighestPossibleValue() throws Throwable {
        UsageCycleResetDayOfMonthPickerDialog dialog =
                UsageCycleResetDayOfMonthPickerDialog.newInstance(32);
        mActivityTestRule.runOnUiThread(() -> {
            dialog.show(mFragmentManager, /* tag= */ null);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(dialog.getSelectedDayOfMonth()).isEqualTo(31);
    }

    @Test
    public void dialogListenerCalled() throws Throwable {
        int setDate = 15;
        UsageCycleResetDayOfMonthPickerDialog dialog =
                UsageCycleResetDayOfMonthPickerDialog.newInstance(setDate);
        dialog.setResetDayOfMonthPickedListener(mResetDayOfMonthPickedListener);
        mActivityTestRule.runOnUiThread(() -> {
            dialog.show(mFragmentManager, /* tag= */ null);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        AlertDialog alertDialog = (AlertDialog) dialog.getDialog();
        mActivityTestRule.runOnUiThread(() -> {
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        verify(mResetDayOfMonthPickedListener).onDayOfMonthPicked(setDate);
    }
}
