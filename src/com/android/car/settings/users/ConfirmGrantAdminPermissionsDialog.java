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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.android.car.settings.R;

/**
 * Dialog to confirm granting of admin permissions.
 */
public class ConfirmGrantAdminPermissionsDialog extends DialogFragment {
    private ConfirmGrantAdminListener mListener;

    /**
     * Sets a listener for onGrantAdminPermissionsConfirmed that will get called if user confirms
     * the dialog.
     *
     * @param listener Instance of {@link ConfirmGrantAdminListener} to call when
     * confirmed.
     */
    public void setConfirmGrantAdminListener(ConfirmGrantAdminListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getString(R.string.grant_admin_permissions_message)
                .concat(System.getProperty("line.separator"))
                .concat(System.getProperty("line.separator"))
                .concat(getString(R.string.action_not_reversible_message));

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.grant_admin_permissions_title)
                .setMessage(message)
                .setPositiveButton(R.string.confirm_grant_admin, (dialog, which) -> {
                    if (mListener != null) {
                        mListener.onGrantAdminPermissionsConfirmed();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    /**
     * Interface for listeners that want to receive a callback when user confirms they want to
     * grant admin permissions to another user.
     */
    public interface ConfirmGrantAdminListener {
        /**
         * Method called only when user presses confirm button.
         */
        void onGrantAdminPermissionsConfirmed();
    }
}
