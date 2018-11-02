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
import android.location.SettingInjectorService;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.NoSetupPreferenceController;
import com.android.settingslib.location.SettingsInjector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Injects Location Services into a {@link PreferenceGroup} with a matching key.
 */
public class LocationServicesPreferenceController extends NoSetupPreferenceController
        implements LifecycleObserver {
    private static final Logger LOG = new Logger(LocationServicesPreferenceController.class);
    private static final IntentFilter INTENT_FILTER_INJECTED_SETTING_CHANGED = new IntentFilter(
            SettingInjectorService.ACTION_INJECTED_SETTING_CHANGED);
    private boolean mIsInjected;
    private final SettingsInjector mSettingsInjector;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOG.i("Received injected settings change intent: " + intent);
            mSettingsInjector.reloadStatusMessages();
        }
    };

    public LocationServicesPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        this(context, preferenceKey, fragmentController, new SettingsInjector(context));
    }

    @VisibleForTesting
    LocationServicesPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, SettingsInjector settingsInjector) {
        super(context, preferenceKey, fragmentController);
        mSettingsInjector = settingsInjector;
    }

    /**
     * Called when the controller is started.
     */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mContext.registerReceiver(mReceiver, INTENT_FILTER_INJECTED_SETTING_CHANGED);
    }

    /**
     * Called when the controller is stopped.
     */
    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        int profileId = UserHandle.USER_CURRENT;
        PreferenceGroup servicesGroup = (PreferenceGroup) screen.findPreference(getPreferenceKey());
        if (!mIsInjected && mSettingsInjector.hasInjectedSettings(profileId)) {
            // If there are injected settings, get and inject them.
            List<Preference> injectedSettings = getSortedInjectedPreferences(profileId);
            for (Preference preference : injectedSettings) {
                servicesGroup.addPreference(preference);
            }
            mIsInjected = true;
        }
        servicesGroup.setVisible(isAvailable() && servicesGroup.getPreferenceCount() > 0);
    }

    private List<Preference> getSortedInjectedPreferences(int profileId) {
        List<Preference> sortedInjections = new ArrayList<>(
                mSettingsInjector.getInjectedSettings(mContext, profileId));
        Collections.sort(sortedInjections);
        return sortedInjections;
    }
}
