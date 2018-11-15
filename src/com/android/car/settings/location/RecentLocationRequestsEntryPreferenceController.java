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

package com.android.car.settings.location;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;

/**
 * Disables Recent Location Requests entry when location is off.
 */
public class RecentLocationRequestsEntryPreferenceController extends NoSetupPreferenceController
        implements LifecycleObserver {
    private static final IntentFilter INTENT_FILTER_LOCATION_MODE_CHANGED =
            new IntentFilter(LocationManager.MODE_CHANGED_ACTION);

    private Preference mPreference;
    private final LocationManager mLocationManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateState(mPreference);
        }
    };

    public RecentLocationRequestsEntryPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
        mLocationManager = (LocationManager) context.getSystemService(Service.LOCATION_SERVICE);
    }

    @Override
    public void updateState(Preference preference) {
        if (mPreference == null) {
            mPreference = preference;
        }
        mPreference.setEnabled(mLocationManager.isLocationEnabled());
    }

    @OnLifecycleEvent(ON_START)
    void onStart() {
        mContext.registerReceiver(mReceiver, INTENT_FILTER_LOCATION_MODE_CHANGED);
    }

    @OnLifecycleEvent(ON_STOP)
    void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }
}
