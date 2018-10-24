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

package com.android.car.settings.datetime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;

import java.util.Calendar;

/**
 * Business logic for the preference which allows for time selection.
 */
public class TimePickerPreferenceController extends NoSetupPreferenceController implements
        LifecycleObserver {

    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPreference == null) {
                throw new IllegalStateException("Preference cannot be null");
            }
            updateState(mPreference);
        }
    };
    private Preference mPreference;

    public TimePickerPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);

        // ACTION_TIME_CHANGED listens to changes to the autoDatetime toggle to update the time.
        // ACTION_TIME_TICK listens to the minute changes to update the shown time.
        // ACTION_TIMEZONE_CHANGED listens to time zone changes to update the shown time.
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_TIME_TICK);
        mIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    }

    /** Starts the broadcast receiver which listens for time changes */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mContext.registerReceiver(mTimeChangeReceiver, mIntentFilter);
    }

    /** Stops the broadcast receiver which listens for time changes */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mTimeChangeReceiver);
    }

    @Override
    public CharSequence getSummary() {
        return DateFormat.getTimeFormat(mContext).format(Calendar.getInstance().getTime());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!autoDatetimeIsEnabled());
    }

    private boolean autoDatetimeIsEnabled() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.AUTO_TIME, 0) > 0;
    }
}
