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
import androidx.preference.SwitchPreference;

import com.android.car.settings.common.NoSetupPreferenceController;

import java.util.Calendar;

/**
 * Business logic for toggle which chooses between 12 hour or 24 hour formats.
 */
public class AutoTimeFormatTogglePreferenceController extends NoSetupPreferenceController
        implements LifecycleObserver, Preference.OnPreferenceChangeListener {
    public static final String HOURS_12 = "12";
    public static final String HOURS_24 = "24";

    private static final int DEMO_MONTH = 11;
    private static final int DEMO_DAY_OF_MONTH = 31;
    private static final int DEMO_HOUR_OF_DAY = 13;
    private static final int DEMO_MINUTE = 0;
    private static final int DEMO_SECOND = 0;
    private final Calendar mTimeFormatDemoDate = Calendar.getInstance();
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

    public AutoTimeFormatTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        // Listens to ACTION_TIME_CHANGED because the description needs to be changed based on
        // the ACTION_TIME_CHANGED intent that this toggle sends.
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
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
        Calendar now = Calendar.getInstance();
        mTimeFormatDemoDate.setTimeZone(now.getTimeZone());
        // We use December 31st because it's unambiguous when demonstrating the date format.
        // We use 13:00 so we can demonstrate the 12/24 hour options.
        mTimeFormatDemoDate.set(now.get(Calendar.YEAR), DEMO_MONTH, DEMO_DAY_OF_MONTH,
                DEMO_HOUR_OF_DAY, DEMO_MINUTE, DEMO_SECOND);
        return DateFormat.getTimeFormat(mContext)
                .format(mTimeFormatDemoDate.getTime());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!(preference instanceof SwitchPreference)) {
            throw new IllegalArgumentException("Expecting SwitchPreference");
        }
        ((SwitchPreference) preference).setChecked(is24Hour());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(preference instanceof SwitchPreference)) {
            throw new IllegalArgumentException("Expecting SwitchPreference");
        }
        boolean isUse24HourFormatEnabled = (boolean) newValue;
        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.TIME_12_24,
                isUse24HourFormatEnabled ? HOURS_24 : HOURS_12);
        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        int timeFormatPreference =
                isUse24HourFormatEnabled ? Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR
                        : Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR;
        timeChanged.putExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT,
                timeFormatPreference);
        mContext.sendBroadcast(timeChanged);
        return true;
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(mContext);
    }
}
