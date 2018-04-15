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

import android.annotation.NonNull;
import android.annotation.StringRes;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.Logger;
import com.android.internal.widget.TextViewInputDisabler;

import java.util.List;

/**
 * Abstract base Activity for choosing a lock password/pin.
 */
public abstract class ChooseLockPasswordBaseFragment extends BaseFragment {

    private static final String FRAGMENT_TAG_SAVE_PASSWORD_WORKER = "save_password_worker";
    private static final Logger LOG = new Logger(ChooseLockPasswordBaseFragment.class);

    // Error code returned from validatePassword(String).
    static final int NO_ERROR = 0;

    private Stage mUiStage = Stage.Introduction;

    private int mUserId;
    private boolean mIsAlphaMode;
    // Password currently in the input field
    private String mEnteredPassword;
    // Existing password that user previously set
    private String mCurrentPassword;
    // Password must be entered twice.  This is what user entered the first time.
    private String mFirstPassword;
    private TextViewInputDisabler mPasswordEntryInputDisabler;
    private SavePasswordWorker mSavePasswordWorker;

    private TextView mHintMessage;
    private Button mSecondaryButton;
    private Button mPrimaryButton;
    private EditText mPasswordEntry;

    private TextChangedHandler mTextChangedHandler = new TextChangedHandler();

    private int mErrorCode = NO_ERROR;

    // Keep track internally of where the user is in choosing a password.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    enum Stage {
        Introduction(
                R.string.choose_lock_password_hints,
                R.string.choose_lock_pin_hints,
                R.string.continue_button_text,
                R.string.lockpassword_cancel_label),

        PasswordInvalid(
                R.string.lockpassword_invalid_password,
                R.string.lockpin_invalid_pin,
                R.string.continue_button_text,
                R.string.lockpassword_clear_label),

        NeedToConfirm(
                R.string.confirm_your_password_header,
                R.string.confirm_your_pin_header,
                R.string.lockpassword_confirm_label,
                R.string.lockpassword_cancel_label),

        ConfirmWrong(
                R.string.confirm_passwords_dont_match,
                R.string.confirm_pins_dont_match,
                R.string.continue_button_text,
                R.string.lockpassword_cancel_label),

        SaveFailure(
                R.string.error_saving_password,
                R.string.error_saving_lockpin,
                R.string.lockscreen_retry_button_text,
                R.string.lockpassword_cancel_label);

        public final int alphaHint;
        public final int numericHint;
        public final int primaryButtonText;
        public final int secondaryButtonText;

        Stage(int hintInAlpha,
                int hintInNumeric,
                int primaryButtonText,
                int secondaryButtonText) {
            this.alphaHint = hintInAlpha;
            this.numericHint = hintInNumeric;
            this.primaryButtonText = primaryButtonText;
            this.secondaryButtonText = secondaryButtonText;
        }

        @StringRes
        public int getHint(boolean isAlpha) {
            if (isAlpha) {
                return alphaHint;
            } else {
                return numericHint;
            }
        }
    }

    /**
     * Returns one of the password quality values defined in {@link DevicePolicyManager}, such
     * as NUMERIC, ALPHANUMERIC etc.
     */
    protected abstract int getPasswordQuality();

    /**
     * Validates PIN/Password and returns the validation result.
     *
     * @param password the raw password the user typed in
     * @return the error code which should be non-zero where there is error. Otherwise
     * {@link #NO_ERROR} should be returned.
     */
    protected abstract int validatePassword(String password);

    /**
     * Converts error code from validatePassword to an array of message describing the error,
     * important message comes first.
     * @param errorCode the code returned by {@link #validatePassword}
     */
    @NonNull
    protected abstract List<String> convertErrorCodeToMessages(int errorCode);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = UserHandle.myUserId();

        int passwordQuality = getPasswordQuality();
        mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == passwordQuality
                || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == passwordQuality
                || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == passwordQuality;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPasswordEntry = view.findViewById(R.id.password_entry);
        mPasswordEntry.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handlePrimaryButtonClick();
                return true;
            }
            return false;
        });

        mPasswordEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // Changing the text while error displayed resets to a normal state
                if (mUiStage == Stage.ConfirmWrong) {
                    mUiStage = Stage.NeedToConfirm;
                } else if (mUiStage == Stage.PasswordInvalid) {
                    mUiStage = Stage.Introduction;
                }
                // Schedule the UI update.
                mTextChangedHandler.notifyAfterTextChanged();
            }
        });

        mPasswordEntry.requestFocus();
        mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

        mHintMessage = view.findViewById(R.id.hint_text);

        // Re-attach to the exiting worker if there is one.
        if (savedInstanceState != null) {
            mSavePasswordWorker = (SavePasswordWorker) getFragmentManager().findFragmentByTag(
                    FRAGMENT_TAG_SAVE_PASSWORD_WORKER);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPrimaryButton = getActivity().findViewById(R.id.action_button1);
        mPrimaryButton.setOnClickListener(view -> handlePrimaryButtonClick());
        mSecondaryButton = getActivity().findViewById(R.id.action_button2);
        mSecondaryButton.setVisibility(View.VISIBLE);
        mSecondaryButton.setOnClickListener(view -> handleSecondaryButtonClick());
    }

    @Override
    public void onStart() {
        super.onStart();
        updateStage(mUiStage);

        if (mSavePasswordWorker != null) {
            setPrimaryButtonEnabled(true);
            mSavePasswordWorker.setListener(this::onChosenLockSaveFinished);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSavePasswordWorker != null) {
            mSavePasswordWorker.setListener(null);
        }
    }

    /**
     * Append the argument to the end of the password entry field
     */
    protected final void appendToPasswordEntry(String text) {
        mPasswordEntry.append(text);
    }

    /**
     * Populate the password entry field with the argument
     */
    protected final void setPasswordEntry(String text) {
        mPasswordEntry.setText(text);
    }

    /**
     * Returns the string in the password entry field
     */
    protected final String getPasswordEntry() {
        return mPasswordEntry.getText().toString();
    }

    private void setPrimaryButtonEnabled(boolean enabled) {
        mPrimaryButton.setEnabled(enabled);
    }

    private void setPrimaryButtonText(@StringRes int textId) {
        mPrimaryButton.setText(textId);
    }

    private void setSecondaryButtonEnabled(boolean enabled) {
        mSecondaryButton.setEnabled(enabled);
    }

    private void setSecondaryButtonText(@StringRes int textId) {
        mSecondaryButton.setText(textId);
    }

    // Updates display message and proceed to next step according to the different text on
    // the primary button.
    protected final void handlePrimaryButtonClick() {
        mEnteredPassword = mPasswordEntry.getText().toString();
        if (TextUtils.isEmpty(mEnteredPassword)) {
            return;
        }

        switch(mUiStage) {
            case Introduction:
                if ((mErrorCode = validatePassword(mEnteredPassword)) == NO_ERROR) {
                    mFirstPassword = mEnteredPassword;
                    mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                } else {
                    updateStage(Stage.PasswordInvalid);
                }
                break;
            case NeedToConfirm:
            case SaveFailure:
                // Password must be entered twice. mFirstPassword is the one the user entered
                // the first time.  mEnteredPassword is what's currently in the input field
                if (mFirstPassword.equals(mEnteredPassword)) {
                    startSaveAndFinish();
                } else {
                    CharSequence tmp = mPasswordEntry.getText();
                    if (tmp != null) {
                        Selection.setSelection((Spannable) tmp, 0, tmp.length());
                    }
                    updateStage(Stage.ConfirmWrong);
                }
                break;
            default:
                // Do nothing.
        }
    }

    // Updates display message and proceed to next step according to the different text on
    // the secondary button.
    private void handleSecondaryButtonClick() {
        if (mSavePasswordWorker != null) {
            return;
        }

        if (mUiStage.secondaryButtonText == R.string.lockpassword_clear_label) {
            mPasswordEntry.setText("");
            mUiStage = Stage.Introduction;
            setSecondaryButtonText(mUiStage.secondaryButtonText);
        } else {
            mFragmentController.goBack();
        }
    }

    private void onChosenLockSaveFinished(boolean isSaveSuccessful) {
        if (isSaveSuccessful) {
            mFragmentController.goBack();

        } else {
            updateStage(Stage.SaveFailure);
        }
    }

    // Starts an async task to save the chosen password.
    private void startSaveAndFinish() {
        if (mSavePasswordWorker != null && !mSavePasswordWorker.isFinished()) {
            LOG.v("startSaveAndFinish with a running SaveAndFinishWorker.");
            return;
        }

        mPasswordEntryInputDisabler.setInputEnabled(false);
        setPrimaryButtonEnabled(false);

        if (mSavePasswordWorker == null) {
            mSavePasswordWorker = new SavePasswordWorker();
            mSavePasswordWorker.setListener(this::onChosenLockSaveFinished);

            getFragmentManager()
                    .beginTransaction()
                    .add(mSavePasswordWorker, FRAGMENT_TAG_SAVE_PASSWORD_WORKER)
                    .commitNow();
        }

        mSavePasswordWorker.start(mUserId, mEnteredPassword, mCurrentPassword,
                getPasswordQuality());
    }

    // Updates the hint message, button text and state
    private void updateUi() {
        boolean inputAllowed = mSavePasswordWorker == null;

        if (mUiStage == Stage.Introduction) {
            String password = mPasswordEntry.getText().toString();
            if (mErrorCode != NO_ERROR) {
                List<String> messages = convertErrorCodeToMessages(mErrorCode);
                // Update the fulfillment of requirements.
                mHintMessage.setText(String.join(" ", messages));
            } else {
                mHintMessage.setText(getString(mUiStage.getHint(mIsAlphaMode)));
            }
            // Enable/Disable the next button accordingly.
            setPrimaryButtonEnabled(!TextUtils.isEmpty(password));
        } else {
            mHintMessage.setText(getString(mUiStage.getHint(mIsAlphaMode)));
            boolean hasPassword = !TextUtils.isEmpty(mEnteredPassword);
            setPrimaryButtonEnabled(inputAllowed && hasPassword);
            setSecondaryButtonEnabled(inputAllowed);
        }

        setPrimaryButtonText(mUiStage.primaryButtonText);
        setSecondaryButtonText(mUiStage.secondaryButtonText);
        mPasswordEntryInputDisabler.setInputEnabled(inputAllowed);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void updateStage(Stage stage) {
        mUiStage = stage;
        updateUi();
    }

    /**
     * Handler that batches text changed events
     */
    class TextChangedHandler extends Handler {
        private static final int ON_TEXT_CHANGED = 1;
        private static final int DELAY_IN_MILLISECOND = 100;

        /**
         * With the introduction of delay, we batch processing the text changed event to reduce
         * unnecessary UI updates.
         */
        private void notifyAfterTextChanged() {
            removeMessages(ON_TEXT_CHANGED);
            sendEmptyMessageDelayed(ON_TEXT_CHANGED, DELAY_IN_MILLISECOND);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ON_TEXT_CHANGED) {
                mErrorCode = NO_ERROR;
                updateUi();
            }
        }
    }
}
