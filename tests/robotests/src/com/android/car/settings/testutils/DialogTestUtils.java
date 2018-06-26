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

package com.android.car.settings.testutils;

import android.widget.Button;

import androidx.fragment.app.DialogFragment;

import com.android.car.settings.R;

/**
 * Helper methods for DialogFragment testing.
 */
public class DialogTestUtils {
    private DialogTestUtils() {}

    /**
     * Invokes onClick on the dialog's positive button.
     */
    public static void clickPositiveButton(DialogFragment dialogFragment) {
        Button positiveButton = (Button) dialogFragment.getDialog().getWindow()
                .findViewById(R.id.positive_button);
        positiveButton.callOnClick();
    }

    /**
     * Invokes onClick on the dialog's negative button.
     */
    public static void  clickNegativeButton(DialogFragment dialogFragment) {
        Button negativeButton = (Button) dialogFragment.getDialog().getWindow()
                .findViewById(R.id.negative_button);
        negativeButton.callOnClick();
    }
}
