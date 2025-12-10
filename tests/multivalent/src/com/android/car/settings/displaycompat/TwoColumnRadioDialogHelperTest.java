/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.displaycompat;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class TwoColumnRadioDialogHelperTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private View mDialogView;
    @Mock
    private Button mApplyButton;
    @Mock
    private Button mCloseButton;
    @Mock
    private RadioGroup mRadioGroup;
    @Mock
    private RadioButton mSelectedRadioButton;
    @Mock
    private Runnable mDismissRunnable;

    private TwoColumnRadioDialogHelper mHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSetFlagsRule.enableFlags(com.android.systemui.car.Flags.FLAG_DISPLAY_COMPATIBILITY_V2);
        setupMockViews();
        mHelper = new TestTwoColumnRadioDialogHelper(mDismissRunnable);
    }

    private void setupMockViews() {
        when(mDialogView.findViewById(R.id.button_apply)).thenReturn(mApplyButton);
        when(mDialogView.findViewById(R.id.button_close)).thenReturn(mCloseButton);
        when(mDialogView.findViewById(R.id.dialog_radio_group)).thenReturn(mRadioGroup);
    }

    @Test
    public void setupDialog_noRadioGroup_throwsIllegalStateException() {
        when(mDialogView.findViewById(R.id.dialog_radio_group)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> {
            mHelper.setupDialog(mDialogView);
        });
    }

    @Test
    public void setupDialog_noApplyButton_throwsIllegalStateException() {
        when(mDialogView.findViewById(R.id.button_apply)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> {
            mHelper.setupDialog(mDialogView);
        });
    }

    @Test
    public void setupDialog_applyButton_isDisabledInitially() {
        when(mRadioGroup.getCheckedRadioButtonId()).thenReturn(-1);

        mHelper.setupDialog(mDialogView);

        verify(mApplyButton).setEnabled(false);
    }

    @Test
    public void setupDialog_applyButton_isEnabledAfterSelection() {
        int selectedRadioButtonId = 4;
        ArgumentCaptor<RadioGroup.OnCheckedChangeListener> listenerCaptor =
                ArgumentCaptor.forClass(RadioGroup.OnCheckedChangeListener.class);
        ArgumentCaptor<Boolean> isEnabledCaptor = ArgumentCaptor.forClass(Boolean.class);

        mHelper.setupDialog(mDialogView);
        verify(mRadioGroup).setOnCheckedChangeListener(listenerCaptor.capture());
        listenerCaptor.getValue().onCheckedChanged(mRadioGroup, selectedRadioButtonId);

        verify(mApplyButton, atLeastOnce()).setEnabled(isEnabledCaptor.capture());
        assertThat(isEnabledCaptor.getAllValues().getLast()).isTrue();
    }

    @Test
    public void setupDialog_closeButton_dismissesDialog() {
        ArgumentCaptor<View.OnClickListener> listenerCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);

        mHelper.setupDialog(mDialogView);
        verify(mCloseButton).setOnClickListener(listenerCaptor.capture());
        listenerCaptor.getValue().onClick(mCloseButton);

        verify(mDismissRunnable).run();
    }

    @Test
    public void setupDialog_applyButton_callsSubmitResultWithSelectedValue() {
        int selectedRadioButtonId = 4;
        String selectedTagValue = "SELECTED_TAG_VALUE";
        when(mDialogView.findViewById(selectedRadioButtonId)).thenReturn(
                mSelectedRadioButton);
        when(mRadioGroup.getCheckedRadioButtonId()).thenReturn(selectedRadioButtonId);
        when(mSelectedRadioButton.getTag()).thenReturn(selectedTagValue);
        ArgumentCaptor<View.OnClickListener> listenerCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);
        TwoColumnRadioDialogHelper spiedHelper = spy(mHelper);

        spiedHelper.setupDialog(mDialogView);
        verify(mApplyButton).setOnClickListener(listenerCaptor.capture());
        listenerCaptor.getValue().onClick(mApplyButton);

        verify(spiedHelper).submitResult(eq(mRadioGroup), eq(selectedTagValue));
    }

    static class TestTwoColumnRadioDialogHelper extends TwoColumnRadioDialogHelper {

        TestTwoColumnRadioDialogHelper(Runnable dismissDialogRunnable) {
            super(dismissDialogRunnable);
        }

        @Override
        protected void setTitle(@NonNull TextView titleView) {
            // no-op
        }

        @Override
        protected void setDescriptionMessage(@NonNull TextView descriptionMessageTextView) {
            // no-op
        }

        @Override
        protected void setupRadioButtons(@NonNull RadioGroup radioGroup) {
            // no-op
        }

        @Override
        protected void setDefaultSelection(@NonNull RadioGroup radioGroup) {
            // no-op
        }

        @Override
        protected void submitResult(@NonNull RadioGroup radioGroup,
                @Nullable String selectedTagValue) {
            // no-op
        }
    }
}
