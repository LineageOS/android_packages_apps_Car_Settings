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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;

import com.android.car.settings.CarSettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

/**
 * Tests for ChooseLockPinActivity class.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class ChooseLockPinActivityTest {
    private ChooseLockPinActivity mActivity;

    @Before
    public void setUpPinActivity() {
        mActivity = Robolectric.buildActivity(ChooseLockPinActivity.class)
                .create()
                .get();
    }

    /**
     * A test to check validatePassword works as expected for pin that contains non digits.
     */
    @Test
    public void testValidatePinContainingNonDigits() {
        String password = "1a34";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPinActivity.CONTAINS_NON_DIGITS);
    }

    /**
     * A test to check validatePassword works as expected for pin with too few digits
     */
    @Test
    public void testValidatePinWithTooFewDigits() {
        String password = "12";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPinActivity.TOO_FEW_DIGITS);
    }

    /**
     * A test to check validatePassword works as expected for numeric complex password
     * that has sequential digits.
     */
    @Test
    public void testValidatePinWithSequentialDigits() {
        ChooseLockPinActivity spyActivity = spy(mActivity);
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX)
                .when(spyActivity)
                .getPasswordQuality();

        String password = "1234";
        assertThat(spyActivity.validatePassword(password))
                .isEqualTo(ChooseLockPinActivity.CONTAINS_SEQUENTIAL_DIGITS);
    }

    /**
     * A test to verify that the activity is finished when save worker succeeds
     */
    @Test
    public void testActivityIsFinishedWhenSaveWorkerSucceeds() {
        ChooseLockPinActivity spyActivity = spy(mActivity);

        Intent intent = new Intent();
        intent.putExtra(SaveChosenLockWorkerBase.EXTRA_KEY_SUCCESS, true);
        spyActivity.onChosenLockSaveFinished(intent);

        verify(spyActivity).finish();
    }

    /**
     * A test to verify that the UI stage is updated when save worker fails
     */
    @Test
    public void testStageIsUpdatedWhenSaveWorkerFails() {
        ChooseLockPinActivity spyActivity = spy(mActivity);
        doNothing().when(spyActivity).updateStage(ChooseLockPinActivity.Stage.SaveFailure);

        Intent intent = new Intent();
        intent.putExtra(SaveChosenLockWorkerBase.EXTRA_KEY_SUCCESS, false);
        spyActivity.onChosenLockSaveFinished(intent);

        verify(spyActivity, never()).finish();
        verify(spyActivity).updateStage(ChooseLockPinActivity.Stage.SaveFailure);
    }
}
