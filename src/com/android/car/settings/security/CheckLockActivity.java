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

import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.car.settings.common.CarSettingActivity;
import com.android.car.settings.common.Logger;
import com.android.internal.widget.LockPatternUtils;

/**
 * Prompts the user to enter their pin, password, or pattern lock (if set) and returns
 * {@link #RESULT_OK} on a successful entry or immediately if the user has no lock setup.
 */
public class CheckLockActivity extends CarSettingActivity implements CheckLockListener {

    private static final Logger LOG = new Logger(CheckLockActivity.class);

    @Override
    @Nullable
    protected Fragment getFragment() {
        Fragment fragment;
        int passwordQuality = new LockPatternUtils(this).getKeyguardStoredPasswordQuality(
                UserHandle.myUserId());
        switch (passwordQuality) {
            case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                // User has not set a password.
                setResult(RESULT_OK);
                finish();
                return null;
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                fragment = ConfirmLockPatternFragment.newInstance(
                        /* isInSetupWizard= */ false);
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                fragment = ConfirmLockPinPasswordFragment.newPinInstance(
                        /* isInSetupWizard= */ false);
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                fragment = ConfirmLockPinPasswordFragment.newPasswordInstance(
                        /* isInSetupWizard= */ false);
                break;
            default:
                LOG.e("Unexpected password quality: " + String.valueOf(passwordQuality));
                fragment = ConfirmLockPinPasswordFragment.newPasswordInstance(
                        /* isInSetupWizard= */ false);
        }

        Bundle bundle = fragment.getArguments();
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putInt(ChooseLockTypeFragment.EXTRA_CURRENT_PASSWORD_QUALITY, passwordQuality);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onLockVerified(byte[] lock) {
        setResult(RESULT_OK);
        finish();
    }
}
