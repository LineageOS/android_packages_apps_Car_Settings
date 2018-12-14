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

package com.android.car.settings.development.debugging;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.android.car.settings.R;

/** Dialog for users to confirm enabling usb debugging. */
public class EnableAdbWarningDialog extends DialogFragment {
    /**
     * Tag used to open and identify the dialog fragment from the FragmentManager or
     * FragmentController.
     */
    public static final String TAG = "EnableAdbWarningDialog";

    private AdbToggleListener mListener;

    /**
     * Sets a listener which determines the action for the positive and negative actions on this
     * dialog.
     */
    public void setAdbToggleListener(AdbToggleListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.adb_warning_title)
                .setMessage(R.string.adb_warning_message)
                .setPositiveButton(android.R.string.yes,
                        (dialog, which) -> mListener.onAdbEnableConfirmed())
                .setNegativeButton(android.R.string.no,
                        (dialog, which) -> mListener.onAdbEnableRejected())
                .create();
    }

    /**
     * Interface for listeners that want a callback when a user selects the positive or negative
     * button on this dialog.
     */
    public interface AdbToggleListener {
        /** Action to take on positive button selected. */
        void onAdbEnableConfirmed();

        /** Action to take on negative button selected. */
        void onAdbEnableRejected();
    }
}
