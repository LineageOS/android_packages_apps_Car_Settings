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

package com.android.car.settings.users;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.android.car.settings.R;

/**
 * Dialog to confirm creation of new user.
 */
public class ConfirmCreateNewUserDialog extends DialogFragment implements
        DialogInterface.OnClickListener {
    private static final String DIALOG_TAG = "ConfirmCreateNewUserDialog";
    private ConfirmCreateNewUserListener mListener;

    /**
     * Interface for listeners that want to receive a callback when user confirms new user creation
     * in the dialog.
     */
    public interface ConfirmCreateNewUserListener {
        void onCreateNewUserConfirmed();
    }

    /**
     * Shows the dialog.
     *
     * @param parent Fragment associated with the dialog.
     */
    public void show(Fragment parent) {
        setTargetFragment(parent, 0);
        show(parent.getFragmentManager(), DIALOG_TAG);
    }

    /**
     * Sets a listener for OnCreateNewUserConfirmed that will get called if user confirms
     * the dialog.
     *
     * @param listener Instance of {@link ConfirmCreateNewUserListener} to call when confirmed.
     */
    public void setConfirmCreateNewUserListener(ConfirmCreateNewUserListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getString(R.string.user_add_user_message_setup)
                .concat(System.getProperty("line.separator"))
                .concat(System.getProperty("line.separator"))
                .concat(getString(R.string.user_add_user_message_update));

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.user_add_user_title)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mListener != null) {
            mListener.onCreateNewUserConfirmed();
        }
    }
}