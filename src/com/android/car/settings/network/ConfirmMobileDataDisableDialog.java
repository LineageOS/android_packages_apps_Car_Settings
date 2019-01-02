/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.network;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.android.car.settings.R;

/** Dialog to confirm disabling mobile data. */
public class ConfirmMobileDataDisableDialog extends DialogFragment implements
        DialogInterface.OnClickListener {

    /**
     * Tag used to open and identify the dialog fragment from the FragmentManager or
     * FragmentController.
     */
    public static final String TAG = "ConfirmMobileDataDisableDialog";

    private ConfirmMobileDataDisableListener mListener;

    /**
     * Sets a listener which will determine how to handle the user action when they press ok or
     * cancel.
     */
    public void setListener(ConfirmMobileDataDisableListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setMessage(R.string.confirm_mobile_data_disable)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mListener != null) {
                mListener.onMobileDataDisableConfirmed();
            }
        }
        if (which == DialogInterface.BUTTON_NEGATIVE) {
            if (mListener != null) {
                mListener.onMobileDataDisableRejected();
            }
        }
    }

    /**
     * Interface for listeners that want to receive a callback when user confirms to disable mobile
     * data.
     */
    public interface ConfirmMobileDataDisableListener {
        /**
         * Method called only when user presses confirm button.
         */
        void onMobileDataDisableConfirmed();

        /**
         * Method called only when user presses cancel button.
         */
        void onMobileDataDisableRejected();
    }
}
