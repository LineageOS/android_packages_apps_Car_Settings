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

package com.android.car.settings.security;

import android.app.Dialog;
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.car.app.CarAlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.internal.widget.LockPatternUtils;

/**
 * Dialog to confirm screen lock removal.
 */
public class ConfirmRemoveScreenLockDialog extends DialogFragment implements
        DialogInterface.OnClickListener {

    private Context mContext;
    private FragmentController mFragmentController;
    private String mCurrentPassword;

    public ConfirmRemoveScreenLockDialog(Context context, FragmentController controller,
            String currentPassword) {
        super();
        mContext = context;
        mFragmentController = controller;
        mCurrentPassword = currentPassword;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new CarAlertDialog.Builder(getContext())
                .setTitle(R.string.remove_screen_lock_title)
                .setBody(R.string.remove_screen_lock_message)
                .setPositiveButton(R.string.remove_button, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int userId = new CarUserManagerHelper(mContext).getCurrentProcessUserId();
        new LockPatternUtils(mContext).clearLock(mCurrentPassword, userId);
        mFragmentController.goBack();
    }
}
