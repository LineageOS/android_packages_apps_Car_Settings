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

package com.android.car.settings.development;

import static com.android.car.settings.development.DevelopmentSettingsUtil.DEVELOPMENT_SETTINGS_CHANGED_ACTION;

import android.car.drivingstate.CarUxRestrictions;
import android.car.userlib.CarUserManagerHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.CallSuper;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Defines the base preference controller which will know how to setup and tear down developer
 * options when it is enabled/disabled.
 *
 * @param <V> the upper bound on the type of {@link Preference} on which the controller expects
 *            to operate.
 */
public abstract class DeveloperOptionsBasePreferenceController<V extends Preference> extends
        PreferenceController<V> {

    private final CarUserManagerHelper mCarUserManagerHelper;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isEnabled = DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(getContext(),
                    mCarUserManagerHelper);
            if (isEnabled) {
                onDeveloperOptionsEnabled();
            } else {
                onDeveloperOptionsDisabled();
            }
        }
    };

    public DeveloperOptionsBasePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mCarUserManagerHelper = new CarUserManagerHelper(context);
    }


    /** Registers a broadcast receiver for when dev settings is enabled/disabled. */
    @Override
    @CallSuper
    protected void onCreateInternal() {
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver,
                new IntentFilter(DEVELOPMENT_SETTINGS_CHANGED_ACTION));
    }

    /** Unregisters a broadcast receiver for when dev settings is enabled/disabled. */
    @Override
    @CallSuper
    protected void onDestroyInternal() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
    }

    /** Called when developer options is enabled. */
    protected void onDeveloperOptionsEnabled() {
    }

    /** Called when developer options is disabled. */
    protected void onDeveloperOptionsDisabled() {
    }
}
