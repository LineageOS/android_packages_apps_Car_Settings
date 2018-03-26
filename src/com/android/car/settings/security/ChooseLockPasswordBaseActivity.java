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

import android.annotation.IdRes;
import android.annotation.NonNull;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
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
import com.android.car.settings.common.Logger;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.TextViewInputDisabler;

/**
 * Abstract base Activity for choosing a lock password/pin.
 */
public abstract class ChooseLockPasswordBaseActivity extends FragmentActivity implements
        SaveChosenLockWorkerBase.Listener {

    protected int mUserId;
    protected Stage mUiStage = Stage.Introduction;

    // Error code returned from validatePassword(String).
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final int NO_ERROR = 0;

    private static final Logger LOG = new Logger(ChooseLockPasswordBaseActivity.class);

    private static final String FRAGMENT_TAG_SAVE_AND_FINISH = "save_and_finish_worker";

    private boolean mIsAlphaMode;
    private String mEnteredPassword;
    private String mCurrentPassword;
    private TextViewInputDisabler mPasswordEntryInputDisabler;

    private SaveLockPasswordWorker mSaveLockPasswordWorker;

    private String mFirstPassword;

    private TextView mDescriptionMessage;
    private TextView mHintMessage;
    private Button mSecondaryButton;
    private Button mPrimaryButton;

    private TextChangedHandler mTextChangedHandler;

    private EditText mPasswordEntry;

    // Keep track internally of where the user is in choosing a password.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    protected enum Stage {
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
     * Returns the resource Id of the layout
     */
    @IdRes
    protected abstract int getLayoutResId();

    /**
     * Returns one of the values defined in {@link DevicePolicyManager}
     */
    protected abstract int getPasswordQuality();

    @Override
    public void onChosenLockSaveFinished(Intent data) {
        boolean isSaveSuccessful =
                data.getBooleanExtra(SaveChosenLockWorkerBase.EXTRA_KEY_SUCCESS, false);
        if (isSaveSuccessful) {
            setResult(RESULT_OK, data);
            finish();
        } else {
            mSaveLockPasswordWorker = null;  // Allow new worker to be created
            updateStage(Stage.SaveFailure);
        }
    }

    /**
     * Validates PIN/Password and returns the validation result.
     *
     * @param password the raw password the user typed in
     * @return the validation result.
     */
    protected int validatePassword(String password) {
        return NO_ERROR;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutResId());

        mTextChangedHandler = new TextChangedHandler();
        mUserId = UserHandle.myUserId();

        int passwordQuality = getPasswordQuality();
        mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == passwordQuality
                || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == passwordQuality
                || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == passwordQuality;

        mPasswordEntry = (EditText) findViewById(R.id.password_entry);
        mPasswordEntry.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handlePrimaryButtonClick(textView);
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
                // Changing the text while error displayed resets to NeedToConfirm state
                if (mUiStage == Stage.ConfirmWrong) {
                    mUiStage = Stage.NeedToConfirm;
                }
                // Schedule the UI update.
                mTextChangedHandler.notifyAfterTextChanged();
            }
        });

        mPasswordEntry.requestFocus();
        mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

        mDescriptionMessage = (TextView) findViewById(R.id.description_text);
        mHintMessage = (TextView) findViewById(R.id.hint_text);

        mPrimaryButton = (Button) findViewById(R.id.footerPrimaryButton);
        mPrimaryButton.setOnClickListener(this::handlePrimaryButtonClick);
        mSecondaryButton = (Button) findViewById(R.id.footerSecondaryButton);
        mSecondaryButton.setOnClickListener(this::handleSecondaryButtonClick);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSaveLockPasswordWorker != null) {
            mSaveLockPasswordWorker.setListener(null);
        }
    }

    protected final void appendToPasswordEntry(String text) {
        mPasswordEntry.append(text);
    }

    protected final void setPasswordEntry(String text) {
        mPasswordEntry.setText(text);
    }

    protected final String getPasswordEntry() {
        return mPasswordEntry.getText().toString();
    }

    private void setPrimaryButtonEnabled(boolean enabled) {
        mPrimaryButton.setEnabled(enabled);
    }

    private void setPrimaryButtonTextId(int textId) {
        mPrimaryButton.setText(textId);
    }

    private void setSecondaryButtonEnabled(boolean enabled) {
        mSecondaryButton.setEnabled(enabled);
    }

    private void setSecondaryButtonTextId(int textId) {
        mSecondaryButton.setText(textId);
    }

    // Updates display message and proceed to next step according to the different text on
    // the secondary button.
    protected void handleSecondaryButtonClick(View unused) {
        if (mSaveLockPasswordWorker != null || TextUtils.isEmpty(mEnteredPassword)) {
            finish();
            return;
        }

        if (mUiStage.secondaryButtonText == R.string.lockpassword_clear_label) {
            mPasswordEntry.setText("");
            mUiStage = Stage.Introduction;
            setSecondaryButtonTextId(mUiStage.secondaryButtonText);
        } else {
            finish();
        }
    }

    // Updates display message and proceed to next step according to the different text on
    // the primary button.
    protected void handlePrimaryButtonClick(View unused) {
        mEnteredPassword = mPasswordEntry.getText().toString();
        if (mSaveLockPasswordWorker != null || TextUtils.isEmpty(mEnteredPassword)) {
            return;
        }

        switch(mUiStage) {
            case Introduction:
                if (validatePassword(mEnteredPassword) == NO_ERROR) {
                    mFirstPassword = mEnteredPassword;
                    mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                } else {
                    updateStage(Stage.PasswordInvalid);
                }
                break;
            case NeedToConfirm:
            case SaveFailure:
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

    // Starts an async task to save the chosen password.
    private void startSaveAndFinish() {
        if (mSaveLockPasswordWorker != null) {
            LOG.v("startSaveAndFinish with an existing SaveAndFinishWorker.");
            return;
        }

        mPasswordEntryInputDisabler.setInputEnabled(false);
        setPrimaryButtonEnabled(false);

        mSaveLockPasswordWorker = new SaveLockPasswordWorker();
        mSaveLockPasswordWorker.setListener(this);

        getFragmentManager().beginTransaction().add(mSaveLockPasswordWorker,
                FRAGMENT_TAG_SAVE_AND_FINISH).commit();
        getFragmentManager().executePendingTransactions();

        mSaveLockPasswordWorker.start(new LockPatternUtils(this),
                mEnteredPassword, mCurrentPassword, getPasswordQuality(), mUserId);
    }

    // Updates the hint based on current Stage and length of password entry
    private void updateUi() {
        boolean inputAllowed = mSaveLockPasswordWorker == null;

        if (mUiStage == Stage.Introduction) {
            final int errorCode = validatePassword(mEnteredPassword);
            String[] messages = convertErrorCodeToMessages(errorCode);
            // Update the fulfillment of requirements.
            mHintMessage.setText(messages.toString());
            // Enable/Disable the next button accordingly.
            setPrimaryButtonEnabled(errorCode == NO_ERROR);
        } else {
            mHintMessage.setText(getString(mUiStage.getHint(mIsAlphaMode)));
            boolean hasPassword = !mEnteredPassword.isEmpty();
            setPrimaryButtonEnabled(inputAllowed && hasPassword);
            setSecondaryButtonEnabled(inputAllowed && hasPassword);
        }

        setPrimaryButtonTextId(mUiStage.primaryButtonText);
        setSecondaryButtonTextId(mUiStage.secondaryButtonText);
        mPasswordEntryInputDisabler.setInputEnabled(inputAllowed);
    }

    @VisibleForTesting
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
        }
    }

    // Converts error code from validatePassword to an array of message,
    // describing the error, important message comes first.
    @NonNull
    protected String[] convertErrorCodeToMessages(int errorCode) {
        return new String[0];
    }
}
