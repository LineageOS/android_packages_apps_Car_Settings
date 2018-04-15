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
import android.support.annotation.VisibleForTesting;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

import java.util.LinkedList;
import java.util.List;

/**
 * Fragment for choosing a lock password.
 */
public class ChooseLockPasswordFragment extends ChooseLockPasswordBaseFragment {

    // Error code returned from validatePassword(String).
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final int CONTAINS_INVALID_CHARACTERS = 1;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final int DOES_NOT_MATCH_PATTERN = 1 << 1;

    /**
     * Password must contain at least one number, one letter,
     * can not have white space, should be between 4 to 8 characters.
     */
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=\\S+$).{4,8}$";

    // Allow non-control Latin-1 characters only.
    private static final String VALID_CHAR_PATTERN = "^[\\x20-\\x7F]*$";

    /**
     * Factory method for creating ChooseLockPatternFragment
     */
    public static ChooseLockPasswordFragment newInstance() {
        ChooseLockPasswordFragment patternFragment = new ChooseLockPasswordFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.security_lock_password);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_LAYOUT, R.layout.choose_lock_password);
        patternFragment.setArguments(bundle);
        return patternFragment;
    }

    @Override
    protected int getPasswordQuality() {
        return DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC;
    }

    @Override
    protected int validatePassword(String password) {
        int errorCode = NO_ERROR;

        if (!password.matches(VALID_CHAR_PATTERN)) {
            errorCode |= CONTAINS_INVALID_CHARACTERS;
        }

        if (!password.matches(PASSWORD_PATTERN)) {
            errorCode |= DOES_NOT_MATCH_PATTERN;
        }

        return errorCode;
    }

    @Override
    protected List<String> convertErrorCodeToMessages(int errorCode) {
        List<String> messages = new LinkedList<>();

        if ((errorCode & CONTAINS_INVALID_CHARACTERS) > 0) {
            messages.add(getString(R.string.lockpassword_illegal_character));
        }

        if ((errorCode & DOES_NOT_MATCH_PATTERN) > 0) {
            messages.add(getString(R.string.lockpassword_invalid_password));
        }

        return messages;
    }
}
