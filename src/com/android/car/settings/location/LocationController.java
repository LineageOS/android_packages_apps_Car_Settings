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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.car.settings.common.Logger;
import com.android.settingslib.Utils;

/**
 * Modifies and listens to location settings changes.
 */
public class LocationController implements LifecycleObserver {

    private static final Logger LOG = new Logger(LocationController.class);
    private static final IntentFilter INTENT_FILTER_LOCATION_MODE_CHANGED =
            new IntentFilter(LocationManager.MODE_CHANGED_ACTION);

    private final Context mContext;
    private final LocationChangeListener mListener;
    private boolean mIsStarted;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOG.i("Received location mode change intent: " + intent);
            if (mListener != null) {
                mListener.onLocationToggled(isEnabled());
            }
        }
    };

    /**
     * Simple listener that listens to changes to location status.
     */
    public interface LocationChangeListener {
        /**
         * Called when location has been toggled.
         *
         * @param enabled {@code true} if the location is now Enabled.
         */
        void onLocationToggled(boolean enabled);
    }

    public LocationController(Context context, @Nullable LocationChangeListener listener) {
        mContext = context;
        mListener = listener;
    }

    @OnLifecycleEvent(ON_START)
    void onStart() {
        mContext.registerReceiver(mReceiver, INTENT_FILTER_LOCATION_MODE_CHANGED);
        mIsStarted = true;
    }

    @OnLifecycleEvent(ON_STOP)
    void onStop() {
        mContext.unregisterReceiver(mReceiver);
        mIsStarted = false;
    }

    /**
     * Enable or Disable location.
     *
     * @param enabled {@code true} to enable location.
     */
    public void setLocationEnabled(boolean enabled) {
        if (mIsStarted) {
            Utils.updateLocationEnabled(mContext, enabled, UserHandle.myUserId(),
                    Settings.Secure.LOCATION_CHANGER_SYSTEM_SETTINGS);
        } else {
            LOG.i("setLocationEnabled: Controller isn't started, location won't be changed.");
        }
    }

    /**
     * Returns {@code true} if the location is enabled.
     */
    public boolean isEnabled() {
        return getLocationMode() != Settings.Secure.LOCATION_MODE_OFF;
    }

    private int getLocationMode() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
    }
}
