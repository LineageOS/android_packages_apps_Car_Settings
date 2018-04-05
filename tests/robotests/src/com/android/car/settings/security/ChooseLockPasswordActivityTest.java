/*
 * Copyright (C) 2018 Google Inc.
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Intent;

import com.android.car.settings.CarSettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

/**
 * Tests for ChooseLockPasswordActivity class.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class ChooseLockPasswordActivityTest {
    private ChooseLockPasswordActivity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(ChooseLockPasswordActivity.class)
                .create()
                .get();
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that are too short.
     */
    @Test
    public void testValidatePasswordTooShort() {
        String password = "lov";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DOES_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that are too long.
     */
    @Test
    public void testValidatePasswordTooLong() {
        String password = "passwordtoolong";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DOES_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that contains white space.
     */
    @Test
    public void testValidatePasswordWhiteSpace() {
        String password = "pass wd";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DOES_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that don't have a digit.
     */
    @Test
    public void testValidatePasswordNoDigit() {
        String password = "password";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DOES_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that contains invalid character.
     */
    @Test
    public void testValidatePasswordNonAscii() {
        String password = "1passwýd";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.CONTAINS_INVALID_CHARACTERS);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that don't have a letter.
     */
    @Test
    public void testValidatePasswordNoLetter() {
        String password = "123456";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DOES_NOT_MATCH_PATTERN);
    }

    /**
     * A test to verify that the activity is finished when save worker succeeds
     */
    @Test
    public void testActivityIsFinishedWhenSaveWorkerSucceeds() {
        ChooseLockPasswordActivity spyActivity = spy(mActivity);

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
        ChooseLockPasswordActivity spyActivity = spy(mActivity);
        doNothing().when(spyActivity).updateStage(ChooseLockPasswordActivity.Stage.SaveFailure);

        Intent intent = new Intent();
        intent.putExtra(SaveChosenLockWorkerBase.EXTRA_KEY_SUCCESS, false);
        spyActivity.onChosenLockSaveFinished(intent);

        verify(spyActivity, never()).finish();
        verify(spyActivity).updateStage(ChooseLockPasswordActivity.Stage.SaveFailure);
    }
}
