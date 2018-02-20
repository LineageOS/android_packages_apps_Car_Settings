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
 * limitations under the License
 */

package com.android.car.settings.security;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordMetrics;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.TextViewInputDisabler;

import com.android.car.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for choosing a lock password/pin.
 */
public class ChooseLockPasswordActivity extends FragmentActivity
        implements OnEditorActionListener, TextWatcher, View.OnClickListener {
    /**
     * Password must contain at least one number, one letter,
     * can not have white space, should be between 4 to 8 characters.
     */
    public static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-zA-Z])(?=\\S+$).{4,8}$";

    protected int mUserId;
    protected Stage mUiStage = Stage.Introduction;

    // Error code returned from validatePassword(String).
    @VisibleForTesting
    static final int NO_ERROR = 0;
    @VisibleForTesting
    static final int CONTAIN_INVALID_CHARACTERS = 1 << 0;
    @VisibleForTesting
    static final int CONTAIN_NON_DIGITS = 1 << 1;
    @VisibleForTesting
    static final int CONTAIN_SEQUENTIAL_DIGITS = 1 << 2;
    @VisibleForTesting
    static final int DO_NOT_MATCH_PATTERN = 1 << 3;
    @VisibleForTesting
    static final int RECENTLY_USED = 1 << 4;
    @VisibleForTesting
    static final int BLACKLISTED = 1 << 5;

    private static final String TAG = "ChooseLockPattern";
    private static final String FRAGMENT_TAG_SAVE_AND_FINISH = "save_and_finish_worker";

    private static final int RESULT_FINISHED = RESULT_FIRST_USER;

    private int mRequestedQuality = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
    private boolean mIsAlphaMode;
    private String mEnteredPassword;
    private String mCurrentPassword;
    private EditText mPasswordEntry;
    private TextViewInputDisabler mPasswordEntryInputDisabler;

    private SaveAndFinishWorker mSaveAndFinishWorker;

    private String mFirstPassword;

    private TextView mDescriptionMessage;
    private TextView mHintMessage;
    private Button mSecondaryButton;
    private Button mPrimaryButton;

    private TextChangedHandler mTextChangedHandler;

    private final SaveAndFinishWorker.Listener mWorkerListener =
            new SaveAndFinishWorker.Listener() {
                @Override
                public void onChosenLockSaveFinished(Intent resultData) {
                    setResult(RESULT_FIRST_USER, resultData);
                }
            };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            setResult(RESULT_FINISHED);
            finish();
        } else {
            mCurrentPassword = data.getStringExtra(SaveChosenLockWorkerBase.EXTRA_KEY_PASSWORD);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        // Changing the text while error displayed resets to NeedToConfirm state
        if (mUiStage == Stage.ConfirmWrong) {
            mUiStage = Stage.NeedToConfirm;
        }
        // Schedule the UI update.
        mTextChangedHandler.notifyAfterTextChanged();
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        // Check if this was the result of hitting the enter or "done" key
        if (actionId == EditorInfo.IME_NULL
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT) {
            handlePrimaryButtonClick();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view == mPrimaryButton) {
            handlePrimaryButtonClick();
        } else if (view == mSecondaryButton) {
            handleSecondaryButtonClick();
        }
    }

    /**
     * Validates PIN/Password and returns the validation result.
     *
     * @param password the raw password the user typed in
     * @return the validation result.
     */
    public int validatePassword(String password) {
        int errorCode = NO_ERROR;

        final PasswordMetrics metrics = PasswordMetrics.computeForPassword(password);

        // Ensure no non-digits if we are requesting numbers. This shouldn't be possible unless
        // user finds some way to bring up soft keyboard.
        if (mRequestedQuality == PASSWORD_QUALITY_NUMERIC
                || mRequestedQuality == PASSWORD_QUALITY_NUMERIC_COMPLEX) {
            if (metrics.letters > 0 || metrics.symbols > 0) {
                errorCode |= CONTAIN_NON_DIGITS;
            }

            if (mRequestedQuality == PASSWORD_QUALITY_NUMERIC_COMPLEX) {
                // Check for repeated characters or sequences (e.g. '1234', '0000', '2468')
                final int sequence = PasswordMetrics.maxLengthSequence(password);
                if (sequence > PasswordMetrics.MAX_ALLOWED_SEQUENCE) {
                    errorCode |= CONTAIN_SEQUENTIAL_DIGITS;
                }
            }
        } else {
            // Allow non-control Latin-1 characters only.
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
                if (c < 32 || c > 127) {
                    errorCode |= CONTAIN_INVALID_CHARACTERS;
                    break;
                }
            }
            if (!password.matches(PASSWORD_PATTERN)) {
                errorCode |= DO_NOT_MATCH_PATTERN;
            }
        }

        return errorCode;
    }

    @VisibleForTesting
    protected void setPasswordQuality(int passwordQuality) {
        mRequestedQuality = passwordQuality;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int quality = intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);
        setPasswordQuality(quality);
        setContentView(R.layout.choose_lock_password);
        ImageView iconImg = (ImageView) findViewById(R.id.base_icon);
        iconImg.setImageResource(R.drawable.ic_lock);

        mTextChangedHandler = new TextChangedHandler();
        mUserId = UserHandle.myUserId();

        mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == mRequestedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == mRequestedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == mRequestedQuality;

        mPasswordEntry = (EditText) findViewById(R.id.password_entry);
        mPasswordEntry.setOnEditorActionListener(this);
        mPasswordEntry.addTextChangedListener(this);
        mPasswordEntry.requestFocus();
        mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

        int currentType = mPasswordEntry.getInputType();
        mPasswordEntry.setInputType(mIsAlphaMode ? currentType
                : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));

        TextView titleText = (TextView) findViewById(R.id.title_text);
        titleText.setText(getString(R.string.set_screen_lock));

        mDescriptionMessage = (TextView) findViewById(R.id.description_text);
        mDescriptionMessage.setText(getString(
                mIsAlphaMode ? R.string.choose_lock_password_message
                        : R.string.choose_lock_pin_message));

        mHintMessage = (TextView) findViewById(R.id.hint_text);
        mHintMessage.setText(getString(
                mIsAlphaMode ? R.string.choose_lock_password_hints
                        : R.string.choose_lock_pin_hints));

        mPrimaryButton = (Button) findViewById(R.id.footerPrimaryButton);
        mPrimaryButton.setOnClickListener(this);
        mSecondaryButton = (Button) findViewById(R.id.footerSecondaryButton);
        mSecondaryButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    // Keep track internally of where the user is in choosing a password.
    private enum Stage {
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
                R.string.lockpassword_cancel_label);

        Stage(int hintInAlpha,
                int hintInNumeric,
                int primaryButtonText,
                int secondaryButtonText) {
            this.alphaHint = hintInAlpha;
            this.numericHint = hintInNumeric;
            this.primaryButtonText = primaryButtonText;
            this.secondaryButtonText = secondaryButtonText;
        }

        public final int alphaHint;
        public final int numericHint;
        public final int primaryButtonText;
        public final int secondaryButtonText;

        @StringRes
        public int getHint(boolean isAlpha) {
            if (isAlpha) {
                return alphaHint;
            } else {
                return numericHint;
            }
        }
    }

    private void setPrimaryButtonEnabled(boolean enabled) {
        mPrimaryButton.setEnabled(enabled);
    }

    private void setPrimaryButtonTextId(int textId) {
        mPrimaryButton.setText(textId);
    }

    private void setSecondaryButtonVisible(boolean visible) {
        mSecondaryButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setSecondaryButtonEnabled(boolean enabled) {
        mSecondaryButton.setEnabled(enabled);
    }

    private void setSecondaryButtonTextId(int textId) {
        mSecondaryButton.setText(textId);
    }

    // Updates display message and proceed to next step according to the different text on
    // the secondary button.
    private void handleSecondaryButtonClick() {
        if (mSaveAndFinishWorker != null || TextUtils.isEmpty(mEnteredPassword)) {
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
    private void handlePrimaryButtonClick() {
        mEnteredPassword = mPasswordEntry.getText().toString();
        if (mSaveAndFinishWorker != null || TextUtils.isEmpty(mEnteredPassword)) {
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
        if (mSaveAndFinishWorker != null) {
            Log.v(TAG, "startSaveAndFinish with an existing SaveAndFinishWorker.");
            return;
        }

        mPasswordEntryInputDisabler.setInputEnabled(false);
        setPrimaryButtonEnabled(false);

        mSaveAndFinishWorker = new SaveAndFinishWorker();
        mSaveAndFinishWorker.setListener(mWorkerListener);

        getFragmentManager().beginTransaction().add(mSaveAndFinishWorker,
                FRAGMENT_TAG_SAVE_AND_FINISH).commit();
        getFragmentManager().executePendingTransactions();

        mSaveAndFinishWorker = new SaveAndFinishWorker();
        mSaveAndFinishWorker.setListener(mWorkerListener);
        mSaveAndFinishWorker.start(new LockPatternUtils(this),
                mEnteredPassword, mCurrentPassword, mRequestedQuality, mUserId);
    }

    // Updates the hint based on current Stage and length of password entry
    private void updateUi() {
        boolean canInput = mSaveAndFinishWorker == null;

        if (mUiStage == Stage.Introduction) {
            final int errorCode = validatePassword(mEnteredPassword);
            String[] messages = convertErrorCodeToMessages(errorCode);
            // Update the fulfillment of requirements.
            mHintMessage.setText(messages.toString());
            // Enable/Disable the next button accordingly.
            setPrimaryButtonEnabled(errorCode == NO_ERROR);
        } else {
            mHintMessage.setText(getString(mUiStage.getHint(mIsAlphaMode)));
            boolean hasPassword = mEnteredPassword == "" ? false : mEnteredPassword.length() > 0;
            setPrimaryButtonEnabled(canInput && hasPassword);
            setSecondaryButtonEnabled(canInput && hasPassword);
        }

        setPrimaryButtonTextId(mUiStage.primaryButtonText);
        setSecondaryButtonTextId(mUiStage.secondaryButtonText);
        mPasswordEntryInputDisabler.setInputEnabled(canInput);
    }

    private void updateStage(Stage stage) {
        mUiStage = stage;
        updateUi();
    }

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
    private String[] convertErrorCodeToMessages(int errorCode) {
        List<String> messages = new ArrayList<>();
        if ((errorCode & CONTAIN_INVALID_CHARACTERS) > 0) {
            messages.add(getString(R.string.lockpassword_illegal_character));
        }
        if ((errorCode & CONTAIN_NON_DIGITS) > 0) {
            messages.add(getString(R.string.lockpassword_pin_contains_non_digits));
        }
        if ((errorCode & CONTAIN_SEQUENTIAL_DIGITS) > 0) {
            messages.add(getString(R.string.lockpassword_pin_no_sequential_digits));
        }
        if ((errorCode & DO_NOT_MATCH_PATTERN) > 0) {
            messages.add(getString(R.string.lockpassword_invalid_password));
        }
        if ((errorCode & RECENTLY_USED) > 0) {
            messages.add(getString((mIsAlphaMode) ? R.string.lockpassword_password_recently_used
                    : R.string.lockpassword_pin_recently_used));
        }
        if ((errorCode & BLACKLISTED) > 0) {
            messages.add(getString((mIsAlphaMode)
                    ? R.string.lockpassword_password_blacklisted_by_admin
                    : R.string.lockpassword_pin_blacklisted_by_admin));
        }
        return messages.toArray(new String[0]);
    }

    /**
     * Worker to store chosen password using LockPatternUtils.
     */
    public static class SaveAndFinishWorker extends SaveChosenLockWorkerBase {

        private String mEnteredPassword;
        private String mCurrentPassword;
        private int mRequestedQuality;

        public void start(LockPatternUtils utils,
                String enteredPassword, String currentPassword, int requestedQuality, int userId) {
            prepare(utils, userId);

            mEnteredPassword = enteredPassword;
            mCurrentPassword = currentPassword;
            mRequestedQuality = requestedQuality;
            mUserId = userId;

            start();
        }

        @Override
        protected Intent saveAndVerifyInBackground() {
            Intent result = null;
            mUtils.saveLockPassword(mEnteredPassword, mCurrentPassword, mRequestedQuality,
                    mUserId);

            return result;
        }
    }
}