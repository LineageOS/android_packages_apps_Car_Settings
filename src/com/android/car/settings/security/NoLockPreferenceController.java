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

import android.app.admin.DevicePolicyManager;
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.internal.widget.LockPatternUtils;

/** Business logic for the no lock preference. */
public class NoLockPreferenceController extends LockTypeBasePreferenceController implements
        LifecycleObserver {

    private static final int[] ALLOWED_PASSWORD_QUALITIES =
            new int[]{DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED};

    @VisibleForTesting
    final ConfirmRemoveScreenLockDialog.ConfirmRemoveScreenLockListener mRemoveLockListener =
            () -> {
                int userId = new CarUserManagerHelper(mContext).getCurrentProcessUserId();
                new LockPatternUtils(mContext).clearLock(getCurrentPassword(), userId);
                getFragmentController().goBack();
            };

    public NoLockPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    /**
     * If the dialog to confirm removal of lock was open previously, make sure the listener is
     * restored.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        ConfirmRemoveScreenLockDialog dialog =
                (ConfirmRemoveScreenLockDialog) getFragmentController().findDialogByTag(
                        ConfirmRemoveScreenLockDialog.TAG);
        if (dialog != null) {
            dialog.setConfirmRemoveScreenLockListener(mRemoveLockListener);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getCurrentPreference() == preference) {
            ConfirmRemoveScreenLockDialog dialog = new ConfirmRemoveScreenLockDialog();
            dialog.setConfirmRemoveScreenLockListener(mRemoveLockListener);
            getFragmentController().showDialog(dialog,
                    ConfirmRemoveScreenLockDialog.TAG);
            return true;
        }
        return false;
    }

    @Override
    protected Fragment fragmentToOpen() {
        // Selecting this preference does not open a new fragment. Instead it opens a dialog to
        // confirm the removal of the existing lock screen.
        return null;
    }

    @Override
    protected int[] allowedPasswordQualities() {
        return ALLOWED_PASSWORD_QUALITIES;
    }
}
