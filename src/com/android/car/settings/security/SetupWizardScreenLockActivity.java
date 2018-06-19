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
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.CarSettingActivity;
import com.android.car.settings.common.Logger;
import com.android.car.settingslib.util.ResultCodes;
import com.android.internal.widget.LockPatternUtils;

/**
 * Entry point Activity for Setup Wizard to set screen lock.
 */
public class SetupWizardScreenLockActivity extends CarSettingActivity implements
        CheckLockListener,
        LockTypeDialogFragment.OnLockSelectListener {

    private static final Logger LOG = new Logger(SetupWizardScreenLockActivity.class);

    private String mCurrLock;
    private int mPasswordQuality;

    @Override
    public void launchFragment(BaseFragment fragment) {
        Bundle args = fragment.getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BaseFragment.EXTRA_RUNNING_IN_SETUP_WIZARD, true);
        if (!TextUtils.isEmpty(mCurrLock)) {
            args.putString(PasswordHelper.EXTRA_CURRENT_SCREEN_LOCK, mCurrLock);
        }
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void goBack() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.suw_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPasswordQuality = new LockPatternUtils(this).getKeyguardStoredPasswordQuality(
                UserHandle.myUserId());

        if (savedInstanceState == null) {
            BaseFragment fragment;
            switch (mPasswordQuality) {
                case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                    // In Setup Wizard, the landing page is always the Pin screen
                    fragment = ChooseLockPinPasswordFragment.newPinInstance(
                            /* isInSetupWizard= */ true);
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    fragment = ConfirmLockPatternFragment.newInstance(
                            /* isInSetupWizard= */ true);
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC: // Fall through
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    fragment = ConfirmLockPinPasswordFragment.newPinInstance(
                            /* isInSetupWizard= */ true);
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC: // Fall through
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                    fragment = ConfirmLockPinPasswordFragment.newPasswordInstance(
                            /* isInSetupWizard= */ true);
                    break;
                default:
                    LOG.e("Unexpected password quality: " + String.valueOf(mPasswordQuality));
                    fragment = ConfirmLockPinPasswordFragment.newPasswordInstance(
                            /* isInSetupWizard= */ true);
            }

            launchFragment(fragment);
        }
    }

    /**
     * Handler that will be invoked when Cancel button is clicked in the fragment.
     */
    public void onCancel() {
        setResult(ResultCodes.RESULT_SKIP);
        finish();
    }

    /**
     * Handler that will be invoked when lock save is completed.
     */
    public void onComplete() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onLockVerified(String lock) {
        mCurrLock = lock;
        // In Setup Wizard, the landing page is always the Pin screen
        BaseFragment fragment = ChooseLockPinPasswordFragment.newPinInstance(
                /* isInSetupWizard= */ true);
        launchFragment(fragment);
    }

    @Override
    public void onLockTypeSelected(int position) {
        BaseFragment fragment = null;

        switch(position) {
            case LockTypeDialogFragment.POSITION_NONE:
                if (mPasswordQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                    new LockPatternUtils(this).clearLock(mCurrLock, UserHandle.myUserId());
                }
                setResult(ResultCodes.RESULT_NONE);
                finish();
                break;
            case LockTypeDialogFragment.POSITION_PIN:
                fragment = ChooseLockPinPasswordFragment.newPinInstance(
                        /* isInSetupWizard= */ true);
                break;
            case LockTypeDialogFragment.POSITION_PATTERN:
                fragment = ChooseLockPatternFragment.newInstance(/* isInSetupWizard= */ true);
                break;
            case LockTypeDialogFragment.POSITION_PASSWORD:
                fragment = ChooseLockPinPasswordFragment.newPasswordInstance(
                        /* isInSetupWizard= */ true);
                break;
            default:
                LOG.e("Lock type position out of bounds");
        }
        if (fragment != null) {
            launchFragment(fragment);
        }
    }
}
