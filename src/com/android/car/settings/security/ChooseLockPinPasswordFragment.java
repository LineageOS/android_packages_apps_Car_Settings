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
 * Fragment for choosing a lock password/pin.
 */
public class ChooseLockPinPasswordFragment extends BaseFragment {

    private static final String LOCK_OPTIONS_DIALOG_TAG = "lock_options_dialog_tag";
    private static final String FRAGMENT_TAG_SAVE_PASSWORD_WORKER = "save_password_worker";
    private static final Logger LOG = new Logger(ChooseLockPinPasswordFragment.class);
    private static final String EXTRA_IS_PIN = "extra_is_pin";

    // View Ids used to set onClick listener
    private static final int[] PIN_PAD_KEYS = { R.id.key0, R.id.key1, R.id.key2, R.id.key3,
            R.id.key4, R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9 };

    private Stage mUiStage = Stage.Introduction;

    private int mUserId;
    private int mErrorCode = PasswordHelper.NO_ERROR;

    private boolean mIsInSetupWizard;
    private boolean mIsPin;
    private boolean mIsAlphaMode;

    // Password currently in the input field
    private String mCurrentEntry;
    // Existing password that user previously set
    private String mExistingPassword;
    // Password must be entered twice.  This is what user entered the first time.
    private String mFirstEntry;

    private TextView mHintMessage;
    private Button mSecondaryButton;
    private Button mPrimaryButton;
    private EditText mPasswordField;

    private TextChangedHandler mTextChangedHandler = new TextChangedHandler();
    private TextViewInputDisabler mPasswordEntryInputDisabler;
    private SavePasswordWorker mSavePasswordWorker;
    private PasswordHelper mPasswordHelper;

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
     * Factory method for creating fragment in password mode
     */
    public static ChooseLockPinPasswordFragment newPasswordInstance() {
        ChooseLockPinPasswordFragment passwordFragment = new ChooseLockPinPasswordFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.security_lock_password);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_LAYOUT, R.layout.choose_lock_password);
        bundle.putBoolean(EXTRA_IS_PIN, false);
        passwordFragment.setArguments(bundle);
        return passwordFragment;
    }

    /**
     * Factory method for creating fragment in Pin mode
     */
    public static ChooseLockPinPasswordFragment newPinInstance() {
        ChooseLockPinPasswordFragment passwordFragment = new ChooseLockPinPasswordFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.security_lock_pin);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_LAYOUT, R.layout.choose_lock_pin);
        bundle.putBoolean(EXTRA_IS_PIN, true);
        passwordFragment.setArguments(bundle);
        return passwordFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = UserHandle.myUserId();

        Bundle args = getArguments();
        if (args != null) {
            mIsInSetupWizard = args.getBoolean(BaseFragment.EXTRA_RUNNING_IN_SETUP_WIZARD);
            mIsPin = args.getBoolean(EXTRA_IS_PIN);
        }

        mPasswordHelper = new PasswordHelper(mIsPin);

        int passwordQuality = mPasswordHelper.getPasswordQuality();
        mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == passwordQuality
                || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == passwordQuality
                || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == passwordQuality;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPasswordField = view.findViewById(R.id.password_entry);
        mPasswordField.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handlePrimaryButtonClick();
                return true;
            }
            return false;
        });

        mPasswordField.addTextChangedListener(new TextWatcher() {
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

        mPasswordField.requestFocus();
        mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordField);

        mHintMessage = view.findViewById(R.id.hint_text);

        if (mIsInSetupWizard) {
            View screenLockOptions = view.findViewById(R.id.screen_lock_options);
            screenLockOptions.setVisibility(View.VISIBLE);
            screenLockOptions.setOnClickListener(v -> {
                new LockTypeDialogFragment().show(getFragmentManager(), LOCK_OPTIONS_DIALOG_TAG);
            });
        }

        if (mIsPin) {
            onPinViewCreated(view);
        }

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
    private void appendToPasswordEntry(String text) {
        mPasswordField.append(text);
    }

    /**
     * Populate the password entry field with the argument
     */
    private void setPasswordField(String text) {
        mPasswordField.setText(text);
    }

    /**
     * Returns the string in the password entry field
     */
    private String getPasswordField() {
        return mPasswordField.getText().toString();
    }

    private void onPinViewCreated(View view) {
        View backspace = view.findViewById(R.id.key_backspace);
        backspace.setOnClickListener(v -> {
            String pin = getPasswordField();
            if (pin.length() > 0) {
                setPasswordField(pin.substring(0, pin.length() - 1));
            }
        });

        View enter = view.findViewById(R.id.key_enter);
        enter.setOnClickListener(v -> handlePrimaryButtonClick());

        for (int keyId : PIN_PAD_KEYS) {
            TextView key = view.findViewById(keyId);
            String digit = key.getTag().toString();
            key.setOnClickListener(v -> appendToPasswordEntry(digit));
        }
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
    private void handlePrimaryButtonClick() {
        mCurrentEntry = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(mCurrentEntry)) {
            return;
        }

        switch(mUiStage) {
            case Introduction:
                mErrorCode = mPasswordHelper.validate(mCurrentEntry);
                if (mErrorCode == PasswordHelper.NO_ERROR) {
                    mFirstEntry = mCurrentEntry;
                    mPasswordField.setText("");
                    updateStage(Stage.NeedToConfirm);
                } else {
                    updateStage(Stage.PasswordInvalid);
                }
                break;
            case NeedToConfirm:
            case SaveFailure:
                // Password must be entered twice. mFirstEntry is the one the user entered
                // the first time.  mCurrentEntry is what's currently in the input field
                if (mFirstEntry.equals(mCurrentEntry)) {
                    startSaveAndFinish();
                } else {
                    CharSequence tmp = mPasswordField.getText();
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
            mPasswordField.setText("");
            mUiStage = Stage.Introduction;
            if (mIsInSetupWizard && mUiStage.secondaryButtonText
                    == R.string.lockpassword_cancel_label) {
                setSecondaryButtonText(R.string.lockscreen_skip_button_text);
            } else {
                setSecondaryButtonText(mUiStage.secondaryButtonText);
            }
        } else {
            if (mIsInSetupWizard) {
                ((SetupWizardScreenLockActivity) getActivity()).onCancel();
            } else {
                mFragmentController.goBack();
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void onChosenLockSaveFinished(boolean isSaveSuccessful) {
        if (isSaveSuccessful) {
            onComplete();
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

        mSavePasswordWorker.start(mUserId, mCurrentEntry, mExistingPassword,
                mPasswordHelper.getPasswordQuality());
    }

    // Updates the hint message, button text and state
    private void updateUi() {
        boolean inputAllowed = mSavePasswordWorker == null;

        if (mUiStage == Stage.Introduction) {
            String password = mPasswordField.getText().toString();
            if (mErrorCode != PasswordHelper.NO_ERROR) {
                List<String> messages = mPasswordHelper.convertErrorCodeToMessages(getContext(),
                        mErrorCode);
                // Update the fulfillment of requirements.
                mHintMessage.setText(String.join(" ", messages));
            } else {
                mHintMessage.setText(getString(mUiStage.getHint(mIsAlphaMode)));
            }
            // Enable/Disable the next button accordingly.
            setPrimaryButtonEnabled(!TextUtils.isEmpty(password));
        } else {
            mHintMessage.setText(getString(mUiStage.getHint(mIsAlphaMode)));
            boolean hasPassword = !TextUtils.isEmpty(mCurrentEntry);
            setPrimaryButtonEnabled(inputAllowed && hasPassword);
            setSecondaryButtonEnabled(inputAllowed);
        }

        setPrimaryButtonText(mUiStage.primaryButtonText);
        if (mIsInSetupWizard && mUiStage.secondaryButtonText
                == R.string.lockpassword_cancel_label) {
            setSecondaryButtonText(R.string.lockscreen_skip_button_text);
        } else {
            setSecondaryButtonText(mUiStage.secondaryButtonText);
        }
        mPasswordEntryInputDisabler.setInputEnabled(inputAllowed);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void updateStage(Stage stage) {
        mUiStage = stage;
        updateUi();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void onComplete() {
        if (mIsInSetupWizard) {
            ((SetupWizardScreenLockActivity) getActivity()).onComplete();
        } else {
            mFragmentController.goBack();
        }
    }

    /**
     * Handler that batches text changed events
     */
    private class TextChangedHandler extends Handler {
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
                mErrorCode = PasswordHelper.NO_ERROR;
                updateUi();
            }
        }
    }
}
