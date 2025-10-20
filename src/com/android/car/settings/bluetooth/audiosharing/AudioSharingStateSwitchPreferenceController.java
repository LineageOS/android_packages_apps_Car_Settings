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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.SharedPreferences;

import com.android.car.settings.common.ColoredSwitchPreference;
import com.android.car.settings.common.FragmentController;

/**
 * Enables/disables audio sharing state via SwitchPreference.
 */
public class AudioSharingStateSwitchPreferenceController extends
        BaseAudioSharingPreferenceController<ColoredSwitchPreference> {

    public AudioSharingStateSwitchPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected void updateState(ColoredSwitchPreference preference) {
        setAudioSharingState(isUserEnabled());
    }

    @Override
    protected boolean handlePreferenceChanged(ColoredSwitchPreference preference, Object newValue) {
        setAudioSharingState((boolean) newValue);
        return true;
    }

    private void setAudioSharingState(boolean userEnabled) {
        if (userEnabled != isUserEnabled()) {
            SharedPreferences.Editor editor = getContext().getSharedPreferences(
                    USER_ENABLE_AUDIO_SHARING_KEY, Context.MODE_PRIVATE).edit();
            editor.putBoolean(USER_ENABLE_AUDIO_SHARING_KEY, userEnabled);
            editor.apply();
        }
        if (getPreference().isChecked() != userEnabled) {
            getPreference().setChecked(userEnabled);
        }
        if (!userEnabled) {
            mLeBroadcastProfile.stopLatestBroadcast();
        }
    }

    @Override
    protected Class<ColoredSwitchPreference> getPreferenceType() {
        return ColoredSwitchPreference.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (isBluetoothStateOn() && isBroadcastAvailable()) {
            return AVAILABLE;
        }
        return AVAILABLE_FOR_VIEWING;
    }
}
