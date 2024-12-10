/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.car.settings.Flags;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Manages Wifi configuration: e.g. monitors wifi states, change wifi setting etc.
 */
public class CarWifiManager implements WifiPickerTracker.WifiPickerTrackerCallback,
        LifecycleObserver {
    private static final String TAG = "CarWifiManager";

    private final Context mContext;
    private final Lifecycle mLifecycle;
    private final List<Listener> mListeners = new ArrayList<>();

    private HandlerThread mWorkerThread;
    @Nullable private WifiPickerTracker mWifiTracker;
    @Nullable private WifiManager mWifiManager;

    public interface Listener {
        /**
         * Something about wifi setting changed.
         */
        void onWifiEntriesChanged();

        /**
         * Called when the state of Wifi has changed, the state will be one of
         * the following.
         *
         * <li>{@link WifiManager#WIFI_STATE_DISABLED}</li>
         * <li>{@link WifiManager#WIFI_STATE_ENABLED}</li>
         * <li>{@link WifiManager#WIFI_STATE_DISABLING}</li>
         * <li>{@link WifiManager#WIFI_STATE_ENABLING}</li>
         * <li>{@link WifiManager#WIFI_STATE_UNKNOWN}</li>
         * <p>
         *
         * @param state The new state of wifi.
         */
        void onWifiStateChanged(int state);
    }

    public CarWifiManager(Context context, Lifecycle lifecycle) {
        mContext = context;
        mLifecycle = lifecycle;
        mLifecycle.addObserver(this);
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWorkerThread = new HandlerThread(TAG
                + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        if (mWifiManager != null) {
            mWifiTracker = WifiUtil.createWifiPickerTracker(lifecycle, context,
                    new Handler(Looper.getMainLooper()), mWorkerThread.getThreadHandler(),
                    /* listener= */ this);
        }
    }

    /**
     * Lifecycle method to clean up worker thread on destroy.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    @MainThread
    public void onDestroy() {
        if (mWorkerThread != null) {
            mWorkerThread.quit();
        }
        mLifecycle.removeObserver(this);
    }

    /**
     * Adds {@link Listener}.
     */
    public boolean addListener(Listener listener) {
        return mListeners.add(listener);
    }

    /**
     * Removes {@link Listener}.
     */
    public boolean removeListener(Listener listener) {
        return mListeners.remove(listener);
    }

    /**
     * Returns the currently connected Wi-Fi entries or an empty list if there is no Wi-Fi
     * network connected.
     */
    public List<WifiEntry> getConnectedWifiEntries() {
        if (mWifiManager != null && mWifiManager.isWifiEnabled() && mWifiTracker != null) {
            return mWifiTracker.getActiveWifiEntries();
        }
        return new ArrayList<>();
    }

    /**
     * Returns a list of all reachable Wi-Fi entries, not including the connected Wi-Fi entry.
     */
    public List<WifiEntry> getAllWifiEntries() {
        return getWifiEntries(false);
    }

    /**
     * Returns a list of saved Wi-Fi entries, not including the connected Wi-Fi entry.
     */
    public List<WifiEntry> getSavedWifiEntries() {
        return getWifiEntries(true);
    }

    private List<WifiEntry> getWifiEntries(boolean onlySaved) {
        List<WifiEntry> wifiEntries = new ArrayList<WifiEntry>();
        if (mWifiManager != null && mWifiManager.isWifiEnabled() && mWifiTracker != null) {
            for (WifiEntry wifiEntry : mWifiTracker.getWifiEntries()) {
                // ignore out of reach Wi-Fi entries.
                if (shouldIncludeWifiEntry(wifiEntry, onlySaved)) {
                    wifiEntries.add(wifiEntry);
                }
            }
        }
        return wifiEntries;
    }

    private boolean shouldIncludeWifiEntry(WifiEntry wifiEntry, boolean onlySaved) {
        boolean reachable = wifiEntry.getLevel() != WifiEntry.WIFI_LEVEL_UNREACHABLE;
        return onlySaved
                ? reachable && wifiEntry.isSaved()
                : reachable;
    }

    /**
     * Returns {@code true} if Wifi is enabled
     */
    public boolean isWifiEnabled() {
        if (mWifiManager != null) {
            return mWifiManager.isWifiEnabled();
        }
        return false;
    }

    /**
     * Returns {@code true} if Wifi tethering is enabled
     */
    public boolean isWifiApEnabled() {
        if (mWifiManager != null) {
            return mWifiManager.isWifiApEnabled();
        }
        return false;
    }

    /**
     * Gets {@link SoftApConfiguration} for tethering
     */
    @Nullable
    public SoftApConfiguration getSoftApConfig() {
        if (mWifiManager != null) {
            return mWifiManager.getSoftApConfiguration();
        }
        return null;
    }

    /**
     * Sets {@link SoftApConfiguration} for tethering
     */
    public void setSoftApConfig(SoftApConfiguration config) {
        if (mWifiManager != null) {
            mWifiManager.setSoftApConfiguration(config);
        }
    }

    /**
     * Gets the country code in ISO 3166 format.
     */
    @Nullable
    public String getCountryCode() {
        if (mWifiManager != null) {
            return mWifiManager.getCountryCode();
        }
        return null;
    }

    /**
     * Checks if the chipset supports 5GHz frequency band.
     */
    public boolean is5GhzBandSupported() {
        if (mWifiManager != null) {
            return mWifiManager.is5GHzBandSupported();
        }
        return false;
    }

    /** Gets the wifi state from {@link WifiManager}. */
    public int getWifiState() {
        if (mWifiManager != null) {
            return mWifiManager.getWifiState();
        }
        return WifiManager.WIFI_STATE_UNKNOWN;
    }

    /** Sets whether wifi is enabled. */
    public boolean setWifiEnabled(boolean enabled) {
        if (mWifiManager != null) {
            return mWifiManager.setWifiEnabled(enabled);
        }
        return false;
    }

    /** Adds callback for Soft AP */
    public void registerSoftApCallback(Executor executor, WifiManager.SoftApCallback callback) {
        if (mWifiManager != null) {
            mWifiManager.registerSoftApCallback(executor, callback);
        }
    }

    /** Removes callback for Soft AP */
    public void unregisterSoftApCallback(WifiManager.SoftApCallback callback) {
        if (mWifiManager != null) {
            mWifiManager.unregisterSoftApCallback(callback);
        }
    }

    /**
     * Returns whether Wi-Fi Dual Band is supported or not.
     */
    @FlaggedApi(Flags.FLAG_HOTSPOT_UI_SPEED_UPDATE)
    public boolean isDualBandSupported() {
        if (mWifiManager != null) {
            return mWifiManager.isBridgedApConcurrencySupported();
        }
        return false;
    }

    @Override
    public void onWifiEntriesChanged() {
        for (Listener listener : mListeners) {
            listener.onWifiEntriesChanged();
        }
    }

    @Override
    public void onNumSavedNetworksChanged() {
    }

    @Override
    public void onNumSavedSubscriptionsChanged() {
    }

    @Override
    public void onWifiStateChanged() {
        if (mWifiTracker != null) {
            int state = mWifiTracker.getWifiState();
            for (Listener listener : mListeners) {
                listener.onWifiStateChanged(state);
            }
        }
    }
}
