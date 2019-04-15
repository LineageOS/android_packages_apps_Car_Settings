/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.settings.common;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;

import com.android.car.settings.R;

/**
 * Presents a dialog with an {@link EditText} associated with an {@link EditTextPreference}.
 *
 * <p>Note: CarSettings needs to use custom dialog implementations in order to launch the platform
 * {@link AlertDialog} instead of the one in the support library.
 */
public class SettingsEditTextPreferenceDialogFragment extends
        SettingsPreferenceDialogFragment implements TextView.OnEditorActionListener {

    private static final String SAVE_STATE_TEXT = "SettingsEditTextPreferenceDialogFragment.text";
    private static final String ARG_INPUT_TYPE = "ARG_INPUT_TYPE";

    private EditText mEditText;

    private CharSequence mText;

    /**
     * Returns a new instance of {@link SettingsEditTextPreferenceDialogFragment} for the {@link
     * EditTextPreference} with the given {@code key} and EditText {@link InputType} specified.
     */
    public static SettingsEditTextPreferenceDialogFragment newInstance(String key,
            int inputType) {
        SettingsEditTextPreferenceDialogFragment fragment =
                new SettingsEditTextPreferenceDialogFragment();
        Bundle b = new Bundle(/* capacity= */ 1);
        b.putString(ARG_KEY, key);
        b.putInt(ARG_INPUT_TYPE, inputType);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mText = getEditTextPreference().getText();
        } else {
            mText = savedInstanceState.getCharSequence(SAVE_STATE_TEXT);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(SAVE_STATE_TEXT, mText);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mEditText = view.findViewById(android.R.id.edit);

        if (mEditText == null) {
            throw new IllegalStateException(
                    "Dialog view must contain an EditText with id @android:id/edit");
        }

        mEditText.requestFocus();
        mEditText.setText(mText);
        // Place cursor at the end
        mEditText.setSelection(mEditText.getText().length());

        int inputType = getArguments().getInt(ARG_INPUT_TYPE);
        if (inputType == InputType.TYPE_CLASS_TEXT) {
            mEditText.setRawInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT
                    | inputType);
        }
        if (inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            revealShowPasswordCheckBox(view);
        }

        mEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mEditText.setOnEditorActionListener(this);
    }

    private EditTextPreference getEditTextPreference() {
        return (EditTextPreference) getPreference();
    }

    @Override
    protected boolean needInputMethod() {
        return true;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = mEditText.getText().toString();
            if (getEditTextPreference().callChangeListener(value)) {
                getEditTextPreference().setText(value);
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            CharSequence newValue = v.getText();

            getEditTextPreference().callChangeListener(newValue);
            dismiss();

            return true;
        }
        return false;
    }

    private void revealShowPasswordCheckBox(View view) {
        CheckBox cb = view.findViewById(R.id.checkbox);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                } else {
                    mEditText.setInputType(InputType.TYPE_CLASS_TEXT
                            | getArguments().getInt(ARG_INPUT_TYPE));
                }
                // Place cursor at the end
                mEditText.setSelection(mEditText.getText().length());
            }
        });
        cb.setVisibility(View.VISIBLE);
    }
}
