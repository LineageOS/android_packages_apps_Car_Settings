/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.bluetooth.audiosharing;

import static android.bluetooth.BluetoothAdapter.ERROR;
import static android.bluetooth.BluetoothAdapter.EXTRA_STATE;
import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_ON;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.car.drivingstate.CarUxRestrictions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.List;

/**
 * Base controller for all audio sharing preferences, which will only have its
 * {@link #getDefaultAvailabilityStatus()} set to return {@code true} if the feature is available on
 * this device and is currently broadcasting.
 *
 * <p>This controller and its subclasses will automatically call {@link #refreshUi} when
 * either bluetooth states or LE broadcast state changes.</p>
 * @param <T> should match the typing of the preference controller.
 */
public abstract class BaseAudioSharingPreferenceController<T extends Preference> extends
        PreferenceController<T> {
    public static final String USER_ENABLE_AUDIO_SHARING_KEY =
            "com.android.car.settings.bluetooth.audiosharing.USER_ENABLE_AUDIO_SHARING";
    protected final LocalBluetoothManager mBtManager;
    protected LeAudioProfile mLeAudioProfile;
    protected LocalBluetoothLeBroadcast mLeBroadcastProfile;
    protected LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistantProfile;
    private boolean mUserAudioSharingEnabled;

    public BaseAudioSharingPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        // required available profiles for LE bluetooth audio
        mBtManager = LocalBluetoothManager.getInstance(context, null);
        if (mBtManager != null && mBtManager.getProfileManager() != null) {
            mLeAudioProfile = mBtManager.getProfileManager().getLeAudioProfile();
            mLeBroadcastProfile = mBtManager.getProfileManager().getLeAudioBroadcastProfile();
            mLeBroadcastAssistantProfile = mBtManager.getProfileManager()
                    .getLeAudioBroadcastAssistantProfile();
        }
        // listener to update availability state bluetooth state is turned on and off
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int state = intent.getIntExtra(EXTRA_STATE, ERROR);
                if (state == STATE_ON || state == STATE_OFF) {
                    refreshUi();
                }
            }
        }, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        // listener to update availability state when broadcast updates
        if (isBroadcastAvailable()) {
            mLeBroadcastProfile.registerServiceCallBack(getContext().getMainExecutor(),
                    new BaseLeBroadcastCallback() {
                        @Override
                        public void onBroadcastStarted(int reason, int broadcastId) {
                            refreshUi();
                            BaseAudioSharingPreferenceController.this
                                    .onBroadcastStartedInternal(broadcastId);
                        }
                        @Override
                        public void onBroadcastStopped(int reason, int broadcastId) {
                            refreshUi();
                            BaseAudioSharingPreferenceController.this
                                    .onBroadcastStoppedInternal(broadcastId);
                        }
                    });
        }
        // listener to update availability state when user toggles audio sharing
        SharedPreferences sharedPrefs =
                context.getSharedPreferences(USER_ENABLE_AUDIO_SHARING_KEY, Context.MODE_PRIVATE);
        mUserAudioSharingEnabled = sharedPrefs.getBoolean(USER_ENABLE_AUDIO_SHARING_KEY,
                /* defaultValue= */ false);
        sharedPrefs.registerOnSharedPreferenceChangeListener(
                (sharedPreferences, key) -> {
                    if (key != null && key.equals(USER_ENABLE_AUDIO_SHARING_KEY)) {
                        mUserAudioSharingEnabled = sharedPrefs.getBoolean(
                                USER_ENABLE_AUDIO_SHARING_KEY, false);
                        refreshUi();
                    }
                });
    }

    /**
     * @return {@link BluetoothLeBroadcastMetadata} representing the current broadcast, or
     * {@code null} if there is no currently active session.
     */
    public BluetoothLeBroadcastMetadata getCurrentBroadcast() {
        List<BluetoothLeBroadcastMetadata> metadata = mLeBroadcastProfile.getAllBroadcastMetadata();
        if (metadata.isEmpty()) {
            return null;
        }
        return metadata.getFirst();
    }

    /**
     * @return {@code true} if bluetooth is turned on.
     */
    public boolean isBluetoothStateOn() {
        return mBtManager != null && mBtManager.getBluetoothAdapter() != null
                && mBtManager.getBluetoothAdapter().getBluetoothState() == STATE_ON;
    }

    /**
     * @return {@code true} if all bluetooth profiles necessary for LE bluetooth broadcast state
     * management in Settings is available, {@code false} otherwise.
     */
    public boolean isBroadcastAvailable() {
        return isBluetoothStateOn() && mBtManager != null && mLeAudioProfile != null
                && mLeBroadcastProfile != null && mLeBroadcastAssistantProfile != null;
    }

    /**
     * @return {@code true} if there is an active broadcast session, {@code false} otherwise.
     */
    public boolean isBroadcasting() {
        return isBroadcastAvailable() && !mLeBroadcastProfile.getAllBroadcastMetadata().isEmpty();
    }

    /**
     * @return {@code true} if user has opted-in to audio sharing.
     */
    public boolean isUserEnabled() {
        return mUserAudioSharingEnabled;
    }

    protected void onBroadcastStartedInternal(int broadcastId) {}

    protected void onBroadcastStoppedInternal(int broadcastId) {}

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (isUserEnabled() && isBroadcasting()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }
}
