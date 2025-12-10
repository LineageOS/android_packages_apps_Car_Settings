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

import static com.android.systemui.car.Flags.displayCompatibilityV2;

import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.settings.R;

/**
 * Helper to help setup a two column dialog with options provided through radio group.
 */
public abstract class TwoColumnRadioDialogHelper {
    private final Runnable mDismissDialogRunnable;

    public TwoColumnRadioDialogHelper(Runnable dismissDialogRunnable) {
        mDismissDialogRunnable = dismissDialogRunnable;
    }

    /**
     * Setup the {@code dialogView} to let the user choose from options using radio group for the
     * given {@code componentName}.
     */
    public void setupDialog(@NonNull View dialogView) {
        if (!displayCompatibilityV2()) {
            return;
        }
        TextView titleTextView = dialogView.findViewById(R.id.dialog_title);
        TextView descriptionMessageTextView = dialogView.findViewById(R.id.dialog_message);

        Button buttonClose = dialogView.findViewById(R.id.button_close);
        Button buttonApply = dialogView.findViewById(R.id.button_apply);
        RadioGroup radioGroup = dialogView.findViewById(R.id.dialog_radio_group);

        if (radioGroup == null || buttonApply == null) {
            throw new IllegalStateException(
                    "Two column radio dialog requires RadioGroup and accept Button to be present.");
        }

        if (titleTextView != null) {
            setTitle(titleTextView);
        }

        if (descriptionMessageTextView != null) {
            setDescriptionMessage(descriptionMessageTextView);
        }

        setupRadioButtons(radioGroup);

        setDefaultSelection(radioGroup);
        // only make the apply button available once a selection has been made
        buttonApply.setEnabled(radioGroup.getCheckedRadioButtonId() != -1);
        radioGroup.setOnCheckedChangeListener(
                (rg, checkedId) -> buttonApply.setEnabled(checkedId != -1));

        if (buttonClose != null) {
            buttonClose.setOnClickListener(view -> mDismissDialogRunnable.run());
        }
        buttonApply.setOnClickListener(view -> {
            int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();
            String selectedTagValue = String.valueOf(Integer.MIN_VALUE);
            RadioButton selectedRadioButton = dialogView.findViewById(selectedRadioButtonId);
            if (selectedRadioButton != null && selectedRadioButton.getTag() != null) {
                selectedTagValue = selectedRadioButton.getTag().toString();
            }
            submitResult(radioGroup, selectedTagValue);
        });
    }

    protected abstract void setTitle(@NonNull TextView titleView);

    protected abstract void setDescriptionMessage(@NonNull TextView descriptionMessageTextView);

    protected abstract void setupRadioButtons(@NonNull RadioGroup radioGroup);

    protected abstract void setDefaultSelection(@NonNull RadioGroup radioGroup);

    protected abstract void submitResult(@NonNull RadioGroup radioGroup,
            @Nullable String selectedTagValue);
}
