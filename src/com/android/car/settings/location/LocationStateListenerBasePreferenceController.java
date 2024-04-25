/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.car.hardware.power.PowerComponent.LOCATION;
import static android.location.LocationManager.EXTRA_ADAS_GNSS_ENABLED;
import static android.location.LocationManager.EXTRA_LOCATION_ENABLED;

import android.car.drivingstate.CarUxRestrictions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PowerPolicyListener;
import com.android.car.settings.common.PreferenceController;

/**
 * Abstract PreferenceController that listens to location and power policy state
 * changes and will refresh the UI when events happen.
 *
 * @param <V> the upper bound on the type of {@link Preference} on which the controller expects
 *         to operate.
 */
public abstract class LocationStateListenerBasePreferenceController<V extends Preference> extends
            PreferenceController<V> {
    protected static final Logger LOG =
            new Logger(LocationStateListenerBasePreferenceController.class);

    /**
     * A Listener which responds to enabling or disabling of {@link
     * LocationManager#MODE_CHANGED_ACTION} and {@link
     * LocationManager#ACTION_ADAS_GNSS_ENABLED_CHANGED} on the device.
     */
    public interface LocationStateListener {

        /**
         * A callback run any time we receive a broadcast stating the location enable state has
         * changed.
         * @param isEnabled Whether or not location is enabled
         */
        void onLocationStateChange(boolean isEnabled);
    }

    private final LocationManager mLocationManager;
    private final BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LocationManager.ACTION_ADAS_GNSS_ENABLED_CHANGED)) {
                boolean isBypassLocationEnabled =
                        intent.getBooleanExtra(EXTRA_ADAS_GNSS_ENABLED, true);
                mBypassLocationStateListener.onLocationStateChange(isBypassLocationEnabled);
            }
            if (intent.getAction().equals(LocationManager.MODE_CHANGED_ACTION)) {
                boolean isMainLocationEnabled =
                        intent.getBooleanExtra(EXTRA_LOCATION_ENABLED, true);
                mMainLocationStateListener.onLocationStateChange(isMainLocationEnabled);
            }
            refreshUi();
        }
    };

    private LocationStateListener mBypassLocationStateListener;
    private LocationStateListener mMainLocationStateListener;
    @VisibleForTesting
    PowerPolicyListener mPowerPolicyListener;
    private boolean mIsPowerPolicyOn = true;

    public LocationStateListenerBasePreferenceController(
            Context context,
            String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mLocationManager = context.getSystemService(LocationManager.class);
    }

    @Override
    protected void onStartInternal() {
        if (mBypassLocationStateListener == null && mMainLocationStateListener == null) {
            return;
        }
        IntentFilter locationChangeFilter = new IntentFilter();
        if (mBypassLocationStateListener != null) {
            locationChangeFilter.addAction(LocationManager.ACTION_ADAS_GNSS_ENABLED_CHANGED);
        }
        if (mMainLocationStateListener != null) {
            locationChangeFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
        }
        getContext().registerReceiver(
                mLocationReceiver, locationChangeFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResumeInternal() {
        if (mPowerPolicyListener != null) {
            mPowerPolicyListener.handleCurrentPolicy();
        }
    }

    @Override
    protected void onStopInternal() {
        if (mBypassLocationStateListener != null || mMainLocationStateListener != null) {
            getContext().unregisterReceiver(mLocationReceiver);
        }
    }

    @Override
    protected void onDestroyInternal() {
        if (mPowerPolicyListener != null) {
            mPowerPolicyListener.release();
        }
    }

    private void setBypassLocationStateListener(LocationStateListener listener) {
        mBypassLocationStateListener = listener;
    }

    /**
     * Add the default bypass location state listener.
     * The default listener triggers a UI refresh when the state changes.
     */
    protected void addDefaultBypassLocationStateListener() {
        setBypassLocationStateListener(isEnabled -> {});
    }

    protected void setMainLocationStateListener(LocationStateListener listener) {
        mMainLocationStateListener = listener;
    }

    /**
     * Add the default main location state listener.
     * The default listener triggers a UI refresh when the state changes.
     */
    protected void addDefaultMainLocationStateListener() {
        setMainLocationStateListener(isEnabled -> {});
    }

    private void setPowerPolicyListener(PowerPolicyListener listener) {
        mPowerPolicyListener = listener;
    }

    /**
     * Add the default location power policy listener.
     * The default listener triggers a UI refresh when the state changes.
     */
    protected void addDefaultPowerPolicyListener() {
        setPowerPolicyListener(new PowerPolicyListener(getContext(), LOCATION,
                isOn -> {
                    mIsPowerPolicyOn = isOn;
                    refreshUi();
                }));
    }

    protected LocationManager getLocationManager() {
        return mLocationManager;
    }

    protected boolean getIsPowerPolicyOn() {
        return mIsPowerPolicyOn;
    }
}

