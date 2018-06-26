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

package com.android.car.settings.users;

import android.app.Dialog;
import android.os.Bundle;

import androidx.car.app.CarAlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.car.settings.R;

/**
 * Dialog to confirm assigning of admin privileges.
 */
public class ConfirmAssignAdminPrivilegesDialog extends DialogFragment {
    private ConfirmAssignAdminListener mListener;

    /**
     * Sets a listener for onAssignAdminPrivilegesConfirmed that will get called if user confirms
     * the dialog.
     *
     * @param listener Instance of {@link ConfirmAssignAdminListener} to call when
     * confirmed.
     */
    public void setConfirmAssignAdminListener(ConfirmAssignAdminListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getString(R.string.assign_admin_privileges_message)
                .concat(System.getProperty("line.separator"))
                .concat(System.getProperty("line.separator"))
                .concat(getString(R.string.action_not_reversible_message));

        return new CarAlertDialog.Builder(getContext())
                .setTitle(R.string.grant_admin_privileges)
                .setBody(message)
                .setPositiveButton(R.string.confirm_assign_admin, (dialog, which) -> {
                    if (mListener != null) {
                        mListener.onAssignAdminConfirmed();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    /**
     * Interface for listeners that want to receive a callback when user confirms they want to
     * assign admin privileges to another user.
     */
    public interface ConfirmAssignAdminListener {
        /**
         * Method called only when user presses confirm button.
         */
        void onAssignAdminConfirmed();
    }
}
