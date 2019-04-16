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

import static com.google.common.truth.Truth.assertThat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.BaseTestActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowWindow;

/** Unit test for {@link SettingsEditTextPreferenceDialogFragment}. */
@RunWith(CarSettingsRobolectricTestRunner.class)
public class SettingsEditTextPreferenceDialogFragmentTest {

    private Context mContext;
    private ActivityController<BaseTestActivity> mTestActivityController;
    private BaseTestActivity mTestActivity;
    private EditTextPreference mPreference;
    private SettingsEditTextPreferenceDialogFragment mFragment;
    private TestTargetFragment mTargetFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mTestActivityController = ActivityController.of(new BaseTestActivity());
        mTestActivity = mTestActivityController.get();
        mTestActivityController.setup();
    }

    @Test
    public void dialogPopulatedWithPreferenceText() {
        finishSetupByInputType(InputType.TYPE_CLASS_TEXT);
        mPreference.setText("text");

        mTestActivity.showDialog(mFragment, /* tag= */ null);
        EditText editTextView = ShadowAlertDialog.getLatestAlertDialog().findViewById(
                android.R.id.edit);

        assertThat(editTextView.getText().toString()).isEqualTo(mPreference.getText());
    }

    @Test
    public void softInputMethodSetOnWindow() {
        finishSetupByInputType(InputType.TYPE_CLASS_TEXT);
        mTestActivity.showDialog(mFragment, /* tag= */ null);

        assertThat(getShadowWindowFromDialog(
                ShadowAlertDialog.getLatestAlertDialog()).getSoftInputMode()).isEqualTo(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Test
    public void editTextHasFocus() {
        finishSetupByInputType(InputType.TYPE_CLASS_TEXT);
        mTestActivity.showDialog(mFragment, /* tag= */ null);
        EditText editTextView = ShadowAlertDialog.getLatestAlertDialog().findViewById(
                android.R.id.edit);

        assertThat(editTextView.hasFocus()).isTrue();
    }

    @Test
    public void onDialogClosed_positiveResult_updatesPreference() {
        finishSetupByInputType(InputType.TYPE_CLASS_TEXT);
        String text = "text";
        mTestActivity.showDialog(mFragment, /* tag= */ null);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        EditText editTextView = dialog.findViewById(android.R.id.edit);
        editTextView.setText(text);

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertThat(mPreference.getText()).isEqualTo(text);
    }

    @Test
    public void onDialogClosed_negativeResult_doesNothing() {
        finishSetupByInputType(InputType.TYPE_CLASS_TEXT);
        mTestActivity.showDialog(mFragment, /* tag= */ null);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        EditText editTextView = dialog.findViewById(android.R.id.edit);
        editTextView.setText("text");

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();

        assertThat(mPreference.getText()).isNull();
    }

    @Test
    public void instanceStateRetained() {
        finishSetupByInputType(InputType.TYPE_CLASS_TEXT);
        String text = "text";
        mPreference.setText(text);
        mTestActivity.showDialog(mFragment, /* tag= */ null);

        // Save instance state.
        Bundle outState = new Bundle();
        mTestActivityController.pause().saveInstanceState(outState).stop();

        // Recreate everything with saved state.
        mTestActivityController = ActivityController.of(new BaseTestActivity());
        mTestActivity = mTestActivityController.get();
        mTestActivityController.setup(outState);

        // Ensure saved text was applied.
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        EditText editTextView = dialog.findViewById(android.R.id.edit);
        assertThat(editTextView.getText().toString()).isEqualTo(text);
    }

    @Test
    public void onStart_inputTypeSetToPassword_shouldRevealShowPasswordCheckBoxUnchecked() {
        finishSetupByInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mTestActivity.showDialog(mFragment, /* tag= */ null);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        CheckBox checkBox = dialog.findViewById(R.id.checkbox);

        assertThat(checkBox.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(!checkBox.isChecked()).isTrue();
    }

    @Test
    public void onStart_inputTypeNotSetToPassword_shouldHideShowPasswordCheckBox() {
        finishSetupByInputType(InputType.TYPE_CLASS_TEXT);
        mTestActivity.showDialog(mFragment, /* tag= */ null);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        CheckBox checkBox = dialog.findViewById(R.id.checkbox);

        assertThat(checkBox.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onCheckBoxChecked_shouldRevealRawPassword() {
        finishSetupByInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        String testPassword = "TEST_PASSWORD";
        mTestActivity.showDialog(mFragment, /* tag= */ null);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        CheckBox checkBox = dialog.findViewById(R.id.checkbox);
        EditText editText = dialog.findViewById(android.R.id.edit);
        editText.setText(testPassword);
        checkBox.performClick();

        assertThat(editText.getInputType()).isEqualTo(InputType.TYPE_CLASS_TEXT);
        assertThat(editText.getText().toString()).isEqualTo(testPassword);
    }

    @Test
    public void onCheckBoxUnchecked_shouldObscureRawPassword() {
        finishSetupByInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        String testPassword = "TEST_PASSWORD";
        mTestActivity.showDialog(mFragment, /* tag= */ null);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        CheckBox checkBox = dialog.findViewById(R.id.checkbox);
        EditText editText = dialog.findViewById(android.R.id.edit);
        editText.setText(testPassword);
        // Performing click twice to simulate uncheck
        checkBox.performClick();
        checkBox.performClick();

        assertThat(editText.getInputType()).isEqualTo((InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        assertThat(editText.getText().toString()).isEqualTo(testPassword);
    }

    private ShadowWindow getShadowWindowFromDialog(AlertDialog dialog) {
        return (ShadowWindow) Shadow.extract(dialog.getWindow());
    }

    /** Extract {@link InputType}-dependent part of the setup to be run in each test. */
    private void finishSetupByInputType(int inputType) {
        TestTargetFragment targetFragment = new TestTargetFragment();
        mTestActivity.launchFragment(targetFragment);

        switch (inputType) {
            case InputType.TYPE_TEXT_VARIATION_PASSWORD:
                mPreference = new PasswordEditTextPreference(mContext);
                break;
            default:
                mPreference = new EditTextPreference(mContext);
        }

        mPreference.setDialogLayoutResource(R.layout.preference_dialog_edittext);
        mPreference.setKey("key");
        targetFragment.getPreferenceScreen().addPreference(mPreference);
        mFragment = SettingsEditTextPreferenceDialogFragment
                .newInstance(mPreference.getKey(), inputType);

        mFragment.setTargetFragment(targetFragment, /* requestCode= */ 0);
    }

    /** Simple {@link PreferenceFragmentCompat} implementation to serve as the target fragment. */
    public static class TestTargetFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        }
    }
}
