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

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
import static com.google.common.truth.Truth.assertThat;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.TestConfig;
import com.android.car.settings.testutils.ShadowActivityManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/**
 * Tests for ChooseLockPasswordActivity class.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {ShadowActivityManager.class})
@Ignore
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
        mActivity.setPasswordQuality(PASSWORD_QUALITY_ALPHANUMERIC);
        String password = "lov";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DO_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that are too long.
     */
    @Test
    public void testValidatePasswordTooLong() {
        mActivity.setPasswordQuality(PASSWORD_QUALITY_ALPHANUMERIC);
        String password = "passwordtoolong";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DO_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that contains white space.
     */
    @Test
    public void testValidatePasswordWhiteSpace() {
        mActivity.setPasswordQuality(PASSWORD_QUALITY_ALPHANUMERIC);
        String password = "pass wd";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DO_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that don't have a digit.
     */
    @Test
    public void testValidatePasswordNoDigit() {
        mActivity.setPasswordQuality(PASSWORD_QUALITY_ALPHANUMERIC);
        String password = "password";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DO_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that contains invalid character.
     */
    @Test
    public void testValidatePasswordNonAscii() {
        mActivity.setPasswordQuality(PASSWORD_QUALITY_ALPHANUMERIC);
        String password = "1passwýd";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.CONTAIN_INVALID_CHARACTERS);
    }

    /**
     * A test to check validatePassword works as expected for alphanumeric password
     * that don't have a letter.
     */
    @Test
    public void testValidatePasswordNoLetter() {
        mActivity.setPasswordQuality(PASSWORD_QUALITY_ALPHANUMERIC);
        String password = "123456";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.DO_NOT_MATCH_PATTERN);
    }

    /**
     * A test to check validatePassword works as expected for numeric complex password
     * that has sequential digits.
     */
    @Test
    public void testValidatePasswordSequentialDigits() {
        mActivity.setPasswordQuality(PASSWORD_QUALITY_NUMERIC_COMPLEX);
        String password = "1234";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.CONTAIN_SEQUENTIAL_DIGITS);
    }

    /**
     * A test to check validatePassword works as expected for numeric password
     * that contains non digits.
     */
    @Test
    public void testValidatePasswordNoneDigits() {
        mActivity.setPasswordQuality(PASSWORD_QUALITY_NUMERIC);
        String password = "1a34";
        assertThat(mActivity.validatePassword(password))
                .isEqualTo(ChooseLockPasswordActivity.CONTAIN_NON_DIGITS);
    }
}
