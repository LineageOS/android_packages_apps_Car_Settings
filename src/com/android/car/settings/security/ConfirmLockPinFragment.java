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

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for confirming existing lock PIN.  The containing activity must implement
 * CheckLockListener.
 */
public class ConfirmLockPinFragment extends BaseFragment {

    private static final String FRAGMENT_TAG_CHECK_LOCK_WORKER = "check_lock_worker";

    // Number of keys in the pin pad
    private static final int NUM_KEYS = 12;

    // View Ids that represent each key in the pin pad
    private static final int[] PIN_PAD_KEYS = { R.id.key0, R.id.key1, R.id.key2, R.id.key3,
            R.id.key4, R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9 };

    private EditText mPasswordField;
    private TextView mMsgView;
    private final List<View> mPinKeys = new ArrayList<>(NUM_KEYS);

    private CheckLockWorker mCheckLockWorker;
    private CheckLockListener mCheckLockListener;

    private int mUserId;
    private String mEnteredPassword;
    private boolean mIsErrorShown;

    /**
     * Factory method for creating ConfirmLockPinFragment.
     */
    public static ConfirmLockPinFragment newInstance() {
        ConfirmLockPinFragment patternFragment = new ConfirmLockPinFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.security_settings_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_LAYOUT, R.layout.confirm_lock_pin_fragment);
        patternFragment.setArguments(bundle);
        return patternFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if ((getActivity() instanceof CheckLockListener)) {
            mCheckLockListener = (CheckLockListener) getActivity();
        } else {
            throw new RuntimeException("The activity must implement CheckLockListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = UserHandle.myUserId();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPasswordField = (EditText) view.findViewById(R.id.password_entry);
        mMsgView = (TextView) view.findViewById(R.id.message);

        for (int keyId : PIN_PAD_KEYS) {
            TextView key = view.findViewById(keyId);
            String digit = key.getTag().toString();
            key.setOnClickListener(v -> {
                if (mIsErrorShown) {
                    clearError();
                }
                mPasswordField.append(digit);
            });
            mPinKeys.add(key);
        }

        View backspace = view.findViewById(R.id.key_backspace);
        backspace.setOnClickListener(v -> {
            if (mIsErrorShown) {
                clearError();
            }
            String pin = mPasswordField.getText().toString();
            if (pin.length() > 0) {
                mPasswordField.setText(pin.substring(0, pin.length() - 1));
            }
        });
        mPinKeys.add(backspace);

        View enter = view.findViewById(R.id.key_enter);
        enter.setOnClickListener(v -> {
            if (mCheckLockWorker == null) {
                mCheckLockWorker = new CheckLockWorker();
                mCheckLockWorker.setListener(ConfirmLockPinFragment.this::onCheckCompleted);

                getFragmentManager()
                        .beginTransaction()
                        .add(mCheckLockWorker, FRAGMENT_TAG_CHECK_LOCK_WORKER)
                        .commitNow();
            }

            setPinPadEnabled(false);
            mEnteredPassword = mPasswordField.getText().toString();
            mCheckLockWorker.checkPinPassword(mUserId, mEnteredPassword);
        });
        mPinKeys.add(enter);

        if (savedInstanceState != null) {
            mCheckLockWorker = (CheckLockWorker) getFragmentManager().findFragmentByTag(
                    FRAGMENT_TAG_CHECK_LOCK_WORKER);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mCheckLockWorker != null) {
            mCheckLockWorker.setListener(this::onCheckCompleted);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mCheckLockWorker != null) {
            mCheckLockWorker.setListener(null);
        }
    }

    private void setPinPadEnabled(boolean enabled) {
        for (View key: mPinKeys) {
            key.setEnabled(enabled);
        }
    }

    private void clearError() {
        mMsgView.setText("");
        mIsErrorShown = false;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void onCheckCompleted(boolean lockMatched) {
        if (lockMatched) {
            mCheckLockListener.onLockVerified(mEnteredPassword);
        } else {
            mMsgView.setText(R.string.lockscreen_wrong_pin);
            mIsErrorShown = true;
            setPinPadEnabled(true);
        }
    }
}
