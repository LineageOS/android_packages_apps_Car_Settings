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
import android.app.admin.PasswordMetrics;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.TextView;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

import java.util.LinkedList;
import java.util.List;

/**
 * Fragment for choosing a lock pin.
 */
public class ChooseLockPinFragment extends ChooseLockPasswordBaseFragment {
    // Error code returned from validatePassword
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final int CONTAINS_NON_DIGITS = 1;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final int CONTAINS_SEQUENTIAL_DIGITS = 1 << 1;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final int TOO_FEW_DIGITS = 1 << 2;

    private static final int MIN_PIN_LENGTH = 4;

    // View Ids used to set onClick listener
    private static final int[] PIN_PAD_KEYS = { R.id.key0, R.id.key1, R.id.key2, R.id.key3,
            R.id.key4, R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9 };

    /**
     * Factory method for creating ChooseLockPatternFragment
     */
    public static ChooseLockPinFragment newInstance() {
        ChooseLockPinFragment patternFragment = new ChooseLockPinFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.security_lock_pin);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_LAYOUT, R.layout.choose_lock_pin);
        patternFragment.setArguments(bundle);
        return patternFragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View backspace = view.findViewById(R.id.key_backspace);
        backspace.setOnClickListener(v -> onBackspaceClick());

        View enter = view.findViewById(R.id.key_enter);
        enter.setOnClickListener(v -> handlePrimaryButtonClick());

        for (int keyId : PIN_PAD_KEYS) {
            TextView key = view.findViewById(keyId);
            String digit = key.getTag().toString();
            key.setOnClickListener(v -> appendToPasswordEntry(digit));
        }
    }

    @Override
    protected int getPasswordQuality() {
        return DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
    }

    @Override
    protected int validatePassword(String password) {
        int errorCode = NO_ERROR;

        PasswordMetrics metrics = PasswordMetrics.computeForPassword(password);
        int passwordQuality = getPasswordQuality();

        if (metrics.length > 0 && metrics.length < MIN_PIN_LENGTH) {
            errorCode |= TOO_FEW_DIGITS;
        }

        if (metrics.letters > 0 || metrics.symbols > 0) {
            errorCode |= CONTAINS_NON_DIGITS;
        }

        if (passwordQuality == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX) {
            // Check for repeated characters or sequences (e.g. '1234', '0000', '2468')
            int sequence = PasswordMetrics.maxLengthSequence(password);
            if (sequence > PasswordMetrics.MAX_ALLOWED_SEQUENCE) {
                errorCode |= CONTAINS_SEQUENTIAL_DIGITS;
            }
        }

        return errorCode;
    }

    @Override
    protected List<String> convertErrorCodeToMessages(int errorCode) {
        List<String> messages = new LinkedList<>();

        if ((errorCode & CONTAINS_NON_DIGITS) > 0) {
            messages.add(getString(R.string.lockpassword_pin_contains_non_digits));
        }

        if ((errorCode & CONTAINS_SEQUENTIAL_DIGITS) > 0) {
            messages.add(getString(R.string.lockpassword_pin_no_sequential_digits));
        }

        if ((errorCode & TOO_FEW_DIGITS) > 0) {
            messages.add(getString(R.string.lockpin_invalid_pin));
        }

        return messages;
    }

    private void onBackspaceClick() {
        String pin = getPasswordEntry();
        if (pin.length() > 0) {
            setPasswordEntry(pin.substring(0, pin.length() - 1));
        }
    }
}
