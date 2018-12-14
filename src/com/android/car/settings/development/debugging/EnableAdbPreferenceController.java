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
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceUtil;
import com.android.car.settings.development.DeveloperOptionsBasePreferenceController;

/** Business logic for enabling adb debugging. */
public class EnableAdbPreferenceController extends
        DeveloperOptionsBasePreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver {

    /** Local broadcast to signify the change in ADB state. */
    public static final String ACTION_ENABLE_ADB_STATE_CHANGED =
            "com.android.car.settings.development.debugging.EnableAdbPreferenceController."
                    + "ENABLE_ADB_STATE_CHANGED";

    private TwoStatePreference mTwoStatePreference;

    private final EnableAdbWarningDialog.AdbToggleListener mListener =
            new EnableAdbWarningDialog.AdbToggleListener() {
                @Override
                public void onAdbEnableConfirmed() {
                    enableAdbSetting(true);
                }

                @Override
                public void onAdbEnableRejected() {
                    updateState(mTwoStatePreference);
                }
            };

    public EnableAdbPreferenceController(Context context,
            String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    /** Reattaches the listener to the confirmation dialog if fragment was recreated. */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        EnableAdbWarningDialog dialog =
                (EnableAdbWarningDialog) getFragmentController().findDialogByTag(
                        EnableAdbWarningDialog.TAG);
        if (dialog != null) {
            dialog.setAdbToggleListener(mListener);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        PreferenceUtil.requirePreferenceType(preference, TwoStatePreference.class);
        mTwoStatePreference = (TwoStatePreference) preference;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mTwoStatePreference.setChecked(isAdbEnabled());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (ActivityManager.isUserAMonkey()) {
            return false;
        }
        PreferenceUtil.requirePreferenceType(preference, TwoStatePreference.class);
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
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED,
                    enabled ? 1 : 0);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                    new Intent(ACTION_ENABLE_ADB_STATE_CHANGED));
        }
    }

    private boolean isAdbEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0)
                == 1;
    }
}
