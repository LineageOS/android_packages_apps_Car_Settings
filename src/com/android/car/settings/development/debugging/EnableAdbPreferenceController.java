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

import android.app.ActivityManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.development.DeveloperOptionsBasePreferenceController;

/** Business logic for enabling adb debugging. */
public class EnableAdbPreferenceController extends
        DeveloperOptionsBasePreferenceController<TwoStatePreference> {

    /** Local broadcast to signify the change in ADB state. */
    public static final String ACTION_ENABLE_ADB_STATE_CHANGED =
            "com.android.car.settings.development.debugging.EnableAdbPreferenceController."
                    + "ENABLE_ADB_STATE_CHANGED";

    @VisibleForTesting
    final EnableAdbWarningDialog.AdbToggleListener mListener =
            new EnableAdbWarningDialog.AdbToggleListener() {
                @Override
                public void onAdbEnableConfirmed() {
                    enableAdbSetting(true);
                }

                @Override
                public void onAdbEnableRejected() {
                    refreshUi();
                }
            };

    public EnableAdbPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<TwoStatePreference> getPreferenceType() {
        return TwoStatePreference.class;
    }

    /** Reattaches the listener to the confirmation dialog if fragment was recreated. */
    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();

        EnableAdbWarningDialog dialog =
                (EnableAdbWarningDialog) getFragmentController().findDialogByTag(
                        EnableAdbWarningDialog.TAG);
        if (dialog != null) {
            dialog.setAdbToggleListener(mListener);
        }
    }

    @Override
    protected void updateState(TwoStatePreference preference) {
        preference.setChecked(isAdbEnabled());
    }

    @Override
    protected boolean handlePreferenceChanged(TwoStatePreference preference, Object newValue) {
        if (ActivityManager.isUserAMonkey()) {
            return false;
        }
        boolean adbEnabled = (Boolean) newValue;
        if (adbEnabled) {
            EnableAdbWarningDialog dialog = new EnableAdbWarningDialog();
            dialog.setAdbToggleListener(mListener);
            getFragmentController().showDialog(dialog, EnableAdbWarningDialog.TAG);
        } else {
            enableAdbSetting(false);
        }
        return true;
    }

    @Override
    protected void onDeveloperOptionsDisabled() {
        enableAdbSetting(false);
    }

    private void enableAdbSetting(boolean enabled) {
        if (isAdbEnabled() != enabled) {
            Settings.Global.putInt(getContext().getContentResolver(), Settings.Global.ADB_ENABLED,
                    enabled ? 1 : 0);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(
                    new Intent(ACTION_ENABLE_ADB_STATE_CHANGED));
        }
    }

    private boolean isAdbEnabled() {
        return Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.ADB_ENABLED, 0) == 1;
    }
}
