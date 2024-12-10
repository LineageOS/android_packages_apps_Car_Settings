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

package com.android.car.settings;

import static android.car.CarOccupantZoneManager.DISPLAY_TYPE_MAIN;

import android.annotation.Nullable;
import android.app.Application;
import android.car.Car;
import android.car.Car.CarServiceLifecycleListener;
import android.car.CarOccupantZoneManager;
import android.car.CarOccupantZoneManager.OccupantZoneConfigChangeListener;
import android.car.CarOccupantZoneManager.OccupantZoneInfo;
import android.car.media.CarAudioManager;
import android.car.wifi.CarWifiManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.view.Display;

import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;

import com.android.car.settings.activityembedding.ActivityEmbeddingRulesController;
import com.android.car.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.car.settings.common.Logger;
import com.android.car.settings.deeplink.DeepLinkHomepageActivity;

/**
 * Application class for CarSettings.
 */
public class CarSettingsApplication extends Application {

    public static final String CAR_SETTINGS_PACKAGE_NAME = "com.android.car.settings";
    private static final Logger LOG = new Logger(CarSettingsApplication.class);
    private CarOccupantZoneManager mCarOccupantZoneManager;

    private final Object mInfoLock = new Object();
    private final Object mCarAudioManagerLock = new Object();
    private final Object mCarWifiManagerLock = new Object();

    @GuardedBy("mInfoLock")
    private int mOccupantZoneDisplayId = Display.DEFAULT_DISPLAY;
    @GuardedBy("mInfoLock")
    private int mAudioZoneId = CarAudioManager.INVALID_AUDIO_ZONE;
    @GuardedBy("mInfoLock")
    private int mOccupantZoneType = CarOccupantZoneManager.OCCUPANT_TYPE_INVALID;
    @GuardedBy("mCarAudioManagerLock")
    private CarAudioManager mCarAudioManager = null;
    @GuardedBy("mCarWifiManagerLock")
    private CarWifiManager mCarWifiManager = null;

    /**
     * Listener to monitor any Occupant Zone configuration change.
     */
    private final OccupantZoneConfigChangeListener mConfigChangeListener = flags -> {
        synchronized (mInfoLock) {
            updateZoneInfoLocked();
        }
    };

    /**
     * Listener to monitor the Lifecycle of car service.
     */
    private final CarServiceLifecycleListener mCarServiceLifecycleListener = (car, ready) -> {
        if (!ready) {
            mCarOccupantZoneManager = null;
            synchronized (mCarAudioManagerLock) {
                mCarAudioManager = null;
            }
            synchronized (mCarWifiManagerLock) {
                mCarWifiManager = null;
            }
            return;
        }
        mCarOccupantZoneManager = (CarOccupantZoneManager) car.getCarManager(
                Car.CAR_OCCUPANT_ZONE_SERVICE);
        if (mCarOccupantZoneManager != null) {
            mCarOccupantZoneManager.registerOccupantZoneConfigChangeListener(
                    mConfigChangeListener);
        }
        synchronized (mCarAudioManagerLock) {
            mCarAudioManager = (CarAudioManager) car.getCarManager(Car.AUDIO_SERVICE);
        }
        synchronized (mCarWifiManagerLock) {
            mCarWifiManager = (CarWifiManager) car.getCarManager(Car.CAR_WIFI_SERVICE);
        }
        synchronized (mInfoLock) {
            updateZoneInfoLocked();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        registerCarServiceLifecycleListener();
        // Register activity embedding only when user is unlocked, which happens after boot has been
        // completed. This is so that RRO values may be read properly for device configurations that
        // uses RROs for embedding related configs, which are not available before boot completion.
        ActivityEmbeddingRulesController controller = new ActivityEmbeddingRulesController(this);
        if (getApplicationContext().getSystemService(UserManager.class).isUserUnlocked()) {
            controller.initActivityEmbeddingRules();
        } else {
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_UNLOCKED);
            getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    controller.initActivityEmbeddingRules();
                }
            }, filter, 0);
        }
        updateDeepLinkHomepageActivityEnabledState();
    }

    @VisibleForTesting
    void registerCarServiceLifecycleListener() {
        Car.createCar(this, /* handler= */ null , Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
                mCarServiceLifecycleListener);
    }

    /**
     * Returns zone type assigned for the current user.
     * The zone type is used to determine whether the settings preferences
     * should be available or not.
     */
    public final int getMyOccupantZoneType() {
        synchronized (mInfoLock) {
            return mOccupantZoneType;
        }
    }

    /**
     * Returns displayId assigned for the current user.
     */
    public final int getMyOccupantZoneDisplayId() {
        synchronized (mInfoLock) {
            return mOccupantZoneDisplayId;
        }
    }

    /**
     * Returns audio zone id assigned for the current user.
     */
    public final int getMyAudioZoneId() {
        synchronized (mInfoLock) {
            return mAudioZoneId;
        }
    }

    /**
     * Returns CarAudioManager instance.
     */
    @Nullable
    public final CarAudioManager getCarAudioManager() {
        synchronized (mCarAudioManagerLock) {
            return mCarAudioManager;
        }
    }

    /**
     * Returns CarAudioManager instance.
     */
    @Nullable
    public final CarWifiManager getCarWifiManager() {
        synchronized (mCarWifiManagerLock) {
            return mCarWifiManager;
        }
    }

    @GuardedBy("mInfoLock")
    private void updateZoneInfoLocked() {
        if (mCarOccupantZoneManager == null) {
            return;
        }
        OccupantZoneInfo info = mCarOccupantZoneManager.getMyOccupantZone();
        if (info != null) {
            mOccupantZoneType = info.occupantType;
            mAudioZoneId = mCarOccupantZoneManager.getAudioZoneIdForOccupant(info);
            Display display = mCarOccupantZoneManager
                    .getDisplayForOccupant(info, DISPLAY_TYPE_MAIN);
            if (display != null) {
                mOccupantZoneDisplayId = display.getDisplayId();
            }
        }
    }

    /**
     * Disable {@link DeepLinkHomepageActivity} if ActivityEmbedding is not enabled.
     */
    private void updateDeepLinkHomepageActivityEnabledState() {
        int componentEnabledState = ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this)
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        try {
            getPackageManager().setComponentEnabledSetting(
                    /* ComponentName */ new ComponentName(this, DeepLinkHomepageActivity.class),
                    /* newState */ componentEnabledState,
                    /* flags */ PackageManager.DONT_KILL_APP);
        } catch (IllegalArgumentException exception) {
            LOG.e("Unable to update enabled state for DeepLinkHomepageActivity: " + exception);
        }
    }
}
