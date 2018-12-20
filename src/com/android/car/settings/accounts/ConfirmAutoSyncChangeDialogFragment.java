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
package com.android.car.settings.accounts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.fragment.app.DialogFragment;

import com.android.car.settings.R;

/**
 * Dialog to inform user about changing auto-sync setting.
 */
public class ConfirmAutoSyncChangeDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {
    private static final String EXTRA_ENABLING = "enabling";
    private static final String EXTRA_USER_HANDLE = "userHandle";
    private boolean mEnabling;
    private UserHandle mUserHandle;
    private OnConfirmListener mListener;

    /** Creates a new ConfirmAutoSyncChangeDialogFragment. */
    public static ConfirmAutoSyncChangeDialogFragment newInstance(boolean enabling,
            UserHandle userHandle, OnConfirmListener listener) {
        ConfirmAutoSyncChangeDialogFragment fragment = new ConfirmAutoSyncChangeDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_ENABLING, enabling);
        bundle.putParcelable(EXTRA_USER_HANDLE, userHandle);
        fragment.setArguments(bundle);
        fragment.setOnConfirmListener(listener);
        return fragment;
    }

    /**
     * Sets a OnConfirmListener that will get called if user confirms
     * the dialog.
     *
     * @param listener Instance of {@link ConfirmAutoSyncChangeDialogFragment.OnConfirmListener} to
     *                 call when confirmed.
     */
    public void setOnConfirmListener(OnConfirmListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mEnabling = savedInstanceState.getBoolean(EXTRA_ENABLING);
            mUserHandle = savedInstanceState.getParcelable(EXTRA_USER_HANDLE);
        } else {
            mEnabling = getArguments().getBoolean(EXTRA_ENABLING);
            mUserHandle = getArguments().getParcelable(EXTRA_USER_HANDLE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        if (mEnabling) {
            builder.setTitle(R.string.data_usage_auto_sync_on_dialog_title);
            builder.setMessage(R.string.data_usage_auto_sync_on_dialog);
        } else {
            builder.setTitle(R.string.data_usage_auto_sync_off_dialog_title);
            builder.setMessage(R.string.data_usage_auto_sync_off_dialog);
        }
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_ENABLING, mEnabling);
        outState.putParcelable(EXTRA_USER_HANDLE, mUserHandle);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            ContentResolver.setMasterSyncAutomaticallyAsUser(mEnabling,
                    mUserHandle.getIdentifier());
            // Now that the user has confirmed, flip the switch
            if (mListener != null) {
                mListener.onConfirm(mEnabling);
            }
        }
    }

    /** Interface for receiving confirmation of an auto sync change. */
    public interface OnConfirmListener {
        /** Called when the user has confirmed an auto sync change. */
        void onConfirm(boolean enabling);
    }
}
