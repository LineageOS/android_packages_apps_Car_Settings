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
 * Dialog to confirm user removal.
 */
public class ConfirmRemoveUserDialog extends DialogFragment {
    private static final String REMOVE_LAST_USER_KEY = "remove_last_user";
    private ConfirmRemoveUserListener mListener;

    public enum UserToRemove { ANY_USER, LAST_USER }

    /**
     * Creates a new {@code ConfirmRemoveUserDialog}.
     *
     * @param userToRemove {@code UserToRemove.LAST_USER} if the user is trying to remove the last
     *                     existing user on the device, {@code UserToRemove.ANY_USER} otherwise.
     */
    public static ConfirmRemoveUserDialog create(UserToRemove userToRemove) {
        ConfirmRemoveUserDialog dialog = new ConfirmRemoveUserDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(REMOVE_LAST_USER_KEY, userToRemove);
        dialog.setArguments(bundle);
        return dialog;
    }
    /**
     * Sets a listener for OnRemoveUserConfirmed that will get called if user confirms
     * the dialog.
     *
     * @param listener Instance of {@link ConfirmRemoveUserListener} to call when confirmed.
     */
    public void setConfirmRemoveUserListener(ConfirmRemoveUserListener listener) {
        mListener = listener;
    }

    private void maybeNotifyRemoveUserListener() {
        if (mListener != null) {
            mListener.onRemoveUserConfirmed();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserToRemove userToRemove = (UserToRemove) getArguments().get(REMOVE_LAST_USER_KEY);
        return userToRemove == UserToRemove.LAST_USER
                ? getRemoveLastUserDialog() : getRemoveAnyUserDialog();
    }

    private Dialog getRemoveAnyUserDialog() {
        String title = getContext().getString(R.string.delete_user_dialog_title);
        String body = getContext().getString(R.string.delete_user_dialog_message);
        return getRemoveUserDialog(title, body);
    }

    private Dialog getRemoveLastUserDialog() {
        String body = getString(R.string.delete_last_user_admin_created_message)
                .concat(System.getProperty("line.separator"))
                .concat(System.getProperty("line.separator"))
                .concat(getString(R.string.delete_last_user_system_setup_required_message));
        String title = getContext().getString(R.string.delete_last_user_dialog_title);
        return getRemoveUserDialog(title, body);
    }

    private Dialog getRemoveUserDialog(String title, String body) {
        return new CarAlertDialog.Builder(getContext())
                .setTitle(title)
                .setBody(body)
                .setPositiveButton(R.string.delete_button, (dialog, which) -> {
                    maybeNotifyRemoveUserListener();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    /**
     * Interface for listeners that want to receive a callback when user confirms user removal in a
     * dialog.
     */
    public interface ConfirmRemoveUserListener {

        /**
         * Method called only when user presses delete button.
         */
        void onRemoveUserConfirmed();
    }
}
