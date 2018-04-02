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

import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.car.settings.R;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.DisplayMode;

import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activity for choosing security lock pattern.
 */
public class ChooseLockPatternActivity extends FragmentActivity implements
        View.OnClickListener, SaveChosenLockWorkerBase.Listener {

    private static final String TAG = "ChooseLockPattern";
    private static final int ID_EMPTY_MESSAGE = -1;
    // How long we wait to clear a wrong pattern
    private int mWrongPatternClearTimeOut;
    private Stage mUiStage = Stage.Introduction;
    private LockPatternView mLockPatternView;
    private TextView mMessageText;
    private Button mSecondaryButton;
    private Button mPrimaryButton;

    private List<LockPatternView.Cell> mChosenPattern;
    private String mCurrentPattern;
    private SaveLockPatternWorker mSaveLockPatternWorker;
    private int mUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.choose_lock_pattern);

        mMessageText = (TextView) findViewById(R.id.description_text);
        mMessageText.setText(getString(R.string.choose_lock_pattern_message));

        mPrimaryButton = (Button) findViewById(R.id.footerPrimaryButton);
        mPrimaryButton.setOnClickListener(this);
        mSecondaryButton = (Button) findViewById(R.id.footerSecondaryButton);
        mSecondaryButton.setOnClickListener(this);

        mLockPatternView = (LockPatternView) findViewById(R.id.lockPattern);
        mLockPatternView.setVisibility(View.VISIBLE);
        mLockPatternView.setEnabled(true);
        mLockPatternView.setFadePattern(false);
        mLockPatternView.clearPattern();
        mLockPatternView.setOnPatternListener(mChooseNewLockPatternListener);

        mUserId = UserHandle.myUserId();
        mCurrentPattern = getIntent()
                .getStringExtra(SaveChosenLockWorkerBase.EXTRA_KEY_PATTERN);

        mWrongPatternClearTimeOut = getResources().getInteger(R.integer.clear_content_timeout_ms);
    }

    @Override
    public void onClick(View view) {
        if (view == mPrimaryButton) {
            handlePrimaryButtonClick();
        } else if (view == mSecondaryButton) {
            handleSecondaryButtonClick();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateStage(mUiStage);

        if (mSaveLockPatternWorker != null) {
            setPrimaryButtonEnabled(true);
            mSaveLockPatternWorker.setListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSaveLockPatternWorker != null) {
            mSaveLockPatternWorker.setListener(null);
        }
    }

    @Override
    public void onChosenLockSaveFinished(Intent data) {
        boolean isSaveSuccessful =
                data.getBooleanExtra(SaveChosenLockWorkerBase.EXTRA_KEY_SUCCESS, false);

        if (isSaveSuccessful) {
            setResult(RESULT_OK, data);
            finish();
        } else {
            mSaveLockPatternWorker = null;  // Allow new worker to be created
            updateStage(Stage.SaveFailure);
        }
    }

    /**
     * Keep track internally of where the user is in choosing a pattern.
     */
    protected enum Stage {

        /**
         * Initial stage when first launching choose a lock pattern.
         * Pattern enabled, secondary button allow for Cancel, primary button disabled.
         */
        Introduction(
                R.string.lockpattern_recording_intro_header,
                SecondaryButtonState.Cancel, PrimaryButtonState.ContinueDisabled,
                true /* patternEnabled */ ),
        /**
         * Help screen to show how a valid pattern looks like.
         * Pattern disabled, primary button shows Ok. No secondary button.
         */
        HelpScreen(
                R.string.lockpattern_settings_help_how_to_record,
                SecondaryButtonState.Gone, PrimaryButtonState.Ok,
                false /* patternEnabled */ ),
        /**
         * Invalid pattern is entered, hint message show required number of dots.
         * Secondary button allows for Retry, primary button disabled.
         */
        ChoiceTooShort(
                R.string.lockpattern_recording_incorrect_too_short,
                SecondaryButtonState.Retry, PrimaryButtonState.ContinueDisabled,
                true /* patternEnabled */ ),
        /**
         * First drawing on the pattern is valid, primary button shows Continue,
         * can proceed to next screen.
         */
        FirstChoiceValid(
                R.string.lockpattern_recording_intro_header,
                SecondaryButtonState.Retry, PrimaryButtonState.Continue,
                false /* patternEnabled */ ),
        /**
         * Need to draw pattern again to confirm.
         * Secondary button allows for Cancel, primary button disabled.
         */
        NeedToConfirm(
                R.string.lockpattern_need_to_confirm,
                SecondaryButtonState.Cancel, PrimaryButtonState.ConfirmDisabled,
                true /* patternEnabled */ ),
        /**
         * Confirmation of previous drawn pattern failed, didn't enter the same pattern.
         * Need to re-draw the pattern to match the fist pattern.
         */
        ConfirmWrong(
                R.string.lockpattern_pattern_wrong,
                SecondaryButtonState.Cancel, PrimaryButtonState.ConfirmDisabled,
                true /* patternEnabled */ ),
        /**
         * Pattern is confirmed after drawing the same pattern twice.
         * Pattern disabled.
         */
        ChoiceConfirmed(
                R.string.lockpattern_pattern_confirmed,
                SecondaryButtonState.Cancel, PrimaryButtonState.Confirm,
                false /* patternEnabled */ ),

        /**
         * Error saving pattern.
         * Pattern disabled, primary button shows Retry, secondary button allows for cancel
         */
        SaveFailure(
                R.string.error_saving_lockpattern,
                SecondaryButtonState.Cancel, PrimaryButtonState.Retry,
                false /* patternEnabled */ );

        final int messageId;
        final SecondaryButtonState secondaryButtonState;
        final PrimaryButtonState primaryButtonState;
        final boolean patternEnabled;

        /**
         * @param message The message displayed as instruction.
         * @param secondaryButtonState The state of the secondary button.
         * @param primaryButtonState The state of the primary button.
         * @param patternEnabled Whether the pattern widget is enabled.
         */
        Stage(int messageId,
                SecondaryButtonState secondaryButtonState,
                PrimaryButtonState primaryButtonState,
                boolean patternEnabled) {
            this.messageId = messageId;
            this.secondaryButtonState = secondaryButtonState;
            this.primaryButtonState = primaryButtonState;
            this.patternEnabled = patternEnabled;
        }
    }

    /**
     * Updates the messages and buttons appropriate to what stage the user
     * is at in choosing a pattern. This doesn't handle clearing out the pattern;
     * the pattern is expected to be in the right state.
     * @param stage The stage UI should be updated to match with.
     */
    protected void updateStage(Stage stage) {
        mUiStage = stage;

        // Message text, visibility and
        // enabled state all known from the stage
        mMessageText.setText(stage.messageId);

        if (stage.secondaryButtonState == SecondaryButtonState.Gone) {
            setSecondaryButtonVisible(false);
        } else {
            setSecondaryButtonVisible(true);
            setSecondaryButtonTextId(stage.secondaryButtonState.textResId);
            setSecondaryButtonEnabled(stage.secondaryButtonState.enabled);
        }

        setPrimaryButtonTextId(stage.primaryButtonState.text);
        setPrimaryButtonEnabled(stage.primaryButtonState.enabled);

        // same for whether the pattern is enabled
        if (stage.patternEnabled) {
            mLockPatternView.enableInput();
        } else {
            mLockPatternView.disableInput();
        }

        // the rest of the stuff varies enough that it is easier just to handle
        // on a case by case basis.
        mLockPatternView.setDisplayMode(DisplayMode.Correct);

        switch (mUiStage) {
            case Introduction:
                mLockPatternView.clearPattern();
                break;
            case HelpScreen:
                mLockPatternView.setPattern(DisplayMode.Animate, mAnimatePattern);
                break;
            case ChoiceTooShort:
                mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                postClearPatternRunnable();
                break;
            case FirstChoiceValid:
                break;
            case NeedToConfirm:
                mLockPatternView.clearPattern();
                break;
            case ConfirmWrong:
                mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                postClearPatternRunnable();
                break;
            case ChoiceConfirmed:
                break;
            default:
                // Do nothing.
        }
    }

    /**
     * The states of the secondary footer button.
     */
    enum SecondaryButtonState {
        Cancel(R.string.lockpattern_cancel_button_text, true),
        CancelDisabled(R.string.lockpattern_cancel_button_text, false),
        Retry(R.string.lockpattern_retry_button_text, true),
        RetryDisabled(R.string.lockpattern_retry_button_text, false),
        Gone(ID_EMPTY_MESSAGE, false);

        /**
         * @param text The displayed text for this mode.
         * @param enabled Whether the button should be enabled.
         */
        SecondaryButtonState(int textId, boolean enabled) {
            this.textResId = textId;
            this.enabled = enabled;
        }

        final int textResId;
        final boolean enabled;
    }

    /**
     * The states of the primary footer button.
     */
    enum PrimaryButtonState {
        Continue(R.string.continue_button_text, true),
        ContinueDisabled(R.string.continue_button_text, false),
        Confirm(R.string.lockpattern_confirm_button_text, true),
        ConfirmDisabled(R.string.lockpattern_confirm_button_text, false),
        Retry(R.string.lockscreen_retry_button_text, true),
        Ok(R.string.okay, true);

        /**
         * @param text The displayed text for this mode.
         * @param enabled Whether the button should be enabled.
         */
        PrimaryButtonState(int text, boolean enabled) {
            this.text = text;
            this.enabled = enabled;
        }

        final int text;
        final boolean enabled;
    }

    // The pattern listener that responds according to a user choosing a new
    // lock pattern.
    private final LockPatternView.OnPatternListener mChooseNewLockPatternListener =
            new LockPatternView.OnPatternListener() {
                @Override
                public void onPatternStart() {
                    mLockPatternView.removeCallbacks(mClearPatternRunnable);
                    updateUIWhenPatternInProgress();
                }

                @Override
                public void onPatternCleared() {
                    mLockPatternView.removeCallbacks(mClearPatternRunnable);
                }

                @Override
                public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                    switch(mUiStage) {
                        case Introduction:
                        case ChoiceTooShort:
                            handlePatternEntered(pattern);
                            break;
                        case ConfirmWrong:
                        case NeedToConfirm:
                            handleConfirmPattern(pattern);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected stage " + mUiStage
                                    + " when entering the pattern.");
                    }
                }

                @Override
                public void onPatternCellAdded(List<Cell> pattern) {}

                private void handleConfirmPattern(List<LockPatternView.Cell> pattern) {
                    if (mChosenPattern == null) {
                        throw new IllegalStateException(
                                "null chosen pattern in stage 'need to confirm");
                    }
                    if (mChosenPattern.equals(pattern)) {
                        updateStage(Stage.ChoiceConfirmed);
                    } else {
                        updateStage(Stage.ConfirmWrong);
                    }
                }

                private void handlePatternEntered(List<LockPatternView.Cell> pattern) {
                    if (pattern.size() < LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
                        updateStage(Stage.ChoiceTooShort);
                    } else {
                        mChosenPattern = new ArrayList<LockPatternView.Cell>(pattern);
                        updateStage(Stage.FirstChoiceValid);
                    }
                }
            };

    private void updateUIWhenPatternInProgress() {
        mMessageText.setText(R.string.lockpattern_recording_inprogress);
        setPrimaryButtonEnabled(false);
        setSecondaryButtonEnabled(false);
    }

    // clear the wrong pattern unless they have started a new one
    // already
    private void postClearPatternRunnable() {
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        mLockPatternView.postDelayed(mClearPatternRunnable, mWrongPatternClearTimeOut);
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

    /**
     * The patten used during the help screen to show how to draw a pattern.
     */
    private final List<LockPatternView.Cell> mAnimatePattern =
            Collections.unmodifiableList(Lists.newArrayList(
                    LockPatternView.Cell.of(0, 0),
                    LockPatternView.Cell.of(0, 1),
                    LockPatternView.Cell.of(1, 1),
                    LockPatternView.Cell.of(2, 1)
            ));

    private Runnable mClearPatternRunnable = () -> mLockPatternView.clearPattern();

    // Update display message and proceed to next step according to the different text on
    // the secondary button.
    private void handleSecondaryButtonClick() {
        switch(mUiStage.secondaryButtonState) {
            case Retry:
                mChosenPattern = null;
                mLockPatternView.clearPattern();
                updateStage(Stage.Introduction);
                break;
            case Cancel:
                finish();
                break;
            default:
                throw new IllegalStateException("secondary footer button pressed, but stage of " +
                        mUiStage + " doesn't make sense");
        }
    }

    // Update display message and decide on next step according to the different text
    // on the primary button
    private void handlePrimaryButtonClick() {
        switch(mUiStage.primaryButtonState) {
            case Continue:
                if (mUiStage != Stage.FirstChoiceValid) {
                    throw new IllegalStateException("expected ui stage "
                            + Stage.FirstChoiceValid + " when button is "
                            + PrimaryButtonState.Continue);
                }
                updateStage(Stage.NeedToConfirm);
                break;
            case Confirm:
                if (mUiStage != Stage.ChoiceConfirmed) {
                    throw new IllegalStateException("expected ui stage " + Stage.ChoiceConfirmed
                            + " when button is " + PrimaryButtonState.Confirm);
                }
                startSaveAndFinish();
                break;
            case Retry:
                if (mUiStage != Stage.SaveFailure) {
                    throw new IllegalStateException("expected ui stage " + Stage.SaveFailure
                            + " when button is " + PrimaryButtonState.Retry);
                }
                startSaveAndFinish();
                break;
            case Ok:
                if (mUiStage != Stage.HelpScreen) {
                    throw new IllegalStateException("Help screen is only mode with ok button, "
                            + "but stage is " + mUiStage);
                }
                mLockPatternView.clearPattern();
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                updateStage(Stage.Introduction);
                break;
            default:
                // Do nothing.
        }
    }

    // Save recorded pattern as an async task and proceed to next
    private void startSaveAndFinish() {
        if (mSaveLockPatternWorker != null) {
            Log.v(TAG, "startSaveAndFinish with an existing SaveLockPatternWorker.");
            return;
        }

        setPrimaryButtonEnabled(false);

        mSaveLockPatternWorker = new SaveLockPatternWorker();
        mSaveLockPatternWorker.setListener(this);
        mSaveLockPatternWorker.start(new LockPatternUtils(this),
                mChosenPattern, mCurrentPattern, mUserId);
    }


}
