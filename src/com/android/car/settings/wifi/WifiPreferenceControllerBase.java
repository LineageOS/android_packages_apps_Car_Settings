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

package com.android.car.settings.wifi;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.car.settings.common.BasePreferenceController;
import com.android.car.settings.common.FragmentController;

/**
 * Abstract controls preference for Wifi.
 */
public abstract class WifiPreferenceControllerBase extends BasePreferenceController
        implements CarWifiManager.Listener, LifecycleObserver {

    @VisibleForTesting
    CarWifiManager mCarWifiManager;

    public WifiPreferenceControllerBase(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    /**
     * Initializes {@link CarWifiManager}. Seprate this function out, so it's easier to test.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void initCarWifiManager() {
        mCarWifiManager = new CarWifiManager(mContext);
    }

    /**
     * Starts listening on {@link CarWifiManager}. Called when Fragment is onStart.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        mCarWifiManager.addListener(this);
        mCarWifiManager.start();
    }

    /**
     * Stops listening on {@link CarWifiManager}. Called when Fragment is onStop.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mCarWifiManager.removeListener(this);
        mCarWifiManager.stop();
    }

    /**
     * Cleans up {@link CarWifiManager}. Called when Fragment is onDestroy.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroyCarWifiManager() {
        mCarWifiManager.destroy();
    }

    @Override
    public int getAvailabilityStatus() {
        return WifiUtil.isWifiAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onAccessPointsChanged() {
        // don't care
    }

    protected CarWifiManager getCarWifiManager() {
        return mCarWifiManager;
    }
}
