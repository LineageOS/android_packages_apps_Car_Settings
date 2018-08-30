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

import androidx.annotation.StringRes;
import androidx.car.app.CarAlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.car.settings.R;

/**
 * Dialog to confirm user removal.
 */
public class ConfirmRemoveUserDialog extends DialogFragment {
    private static final String REMOVE_LAST_USER_KEY = "remove_last_user";

    private ConfirmRemoveUserListener mListener;

    /**
     * Describes the type of user we're trying to remove from the device.
     *
     * LAST_ADMIN = removing the last admin on the device.
     * LAST_USER = removing the last user on the device.
     * ANY_USER = default case; removing admin but other admins are present, or removing a non-admin
     * but other users exist on the device.
     */
    private enum UserType { ANY_USER, LAST_USER, LAST_ADMIN }

    /**
     * Create dialog for removing the last user on the device.
     */
    public static ConfirmRemoveUserDialog createForLastUser(ConfirmRemoveUserListener listener) {
        return create(UserType.LAST_USER, listener);
    }

    /**
     * Create dialog for removing the last admin on the device.
     */
    public static ConfirmRemoveUserDialog createForLastAdmin(ConfirmRemoveUserListener listener) {
        return create(UserType.LAST_ADMIN, listener);
    }

    /**
     * Create dialog for removing a user on the device.
     */
    public static ConfirmRemoveUserDialog createDefault(ConfirmRemoveUserListener listener) {
        return create(UserType.ANY_USER, listener);
    }

    /**
     * Creates a new {@code ConfirmRemoveUserDialog}.
     */
    private static ConfirmRemoveUserDialog create(UserType userType,
            ConfirmRemoveUserListener listener) {
        ConfirmRemoveUserDialog dialog = new ConfirmRemoveUserDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(REMOVE_LAST_USER_KEY, userType);
        dialog.setArguments(bundle);
        dialog.setConfirmRemoveUserListener(listener);
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
        UserType userToRemove = (UserType) getArguments().get(REMOVE_LAST_USER_KEY);

        switch (userToRemove) {
            case LAST_USER:
                return getRemoveLastUserDialog();
            case LAST_ADMIN:
                return getChooseNewAdminDialog();
            default:
                return getRemoveAnyUserDialog();
        }
    }

    private Dialog getRemoveLastUserDialog() {
        String body = getString(R.string.delete_last_user_admin_created_message)
                .concat(System.getProperty("line.separator"))
                .concat(System.getProperty("line.separator"))
                .concat(getString(R.string.delete_last_user_system_setup_required_message));

        return getDialogBuilder(R.string.delete_button)
                .setTitle(R.string.delete_last_user_dialog_title)
                .setBody(body)
                .create();
    }

    private Dialog getChooseNewAdminDialog() {
        return getDialogBuilder(R.string.choose_new_admin_label)
                .setTitle(R.string.choose_new_admin_title)
                .setBody(R.string.choose_new_admin_message)
                .create();
    }

    private Dialog getRemoveAnyUserDialog() {
        return getDialogBuilder(R.string.delete_button)
                .setTitle(R.string.delete_user_dialog_title)
                .setBody(R.string.delete_user_dialog_message)
                .create();
    }

    private CarAlertDialog.Builder getDialogBuilder(@StringRes int posButtonLabelId) {
        return new CarAlertDialog.Builder(getContext())
                .setPositiveButton(posButtonLabelId, (dialog, which) -> {
                    maybeNotifyRemoveUserListener();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null);
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
