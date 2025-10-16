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
package com.android.car.settings.sound.audiorouting;

import android.car.drivingstate.CarUxRestrictions;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.media.AudioDeviceInfo;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.Flags;
import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.sound.AudioRoutePickerFragment;
import com.android.car.settings.sound.AudioRouteSelectionDialogFragment;

/**
 * Controller for launching {@link com.android.car.settings.sound.AudioRoutePickerFragment}
 * <p>The class should also show the current active media output device name.</p>
 */
public class AudioRouteController extends PreferenceController<Preference> {
    private final CarAudioManager mCarAudioManager;
    private final int mAudioZone;
    private final int mMediaUsage;
    @VisibleForTesting
    static final String CONFIRM_CLEAR_STORAGE_DIALOG_TAG =
            "com.android.car.settings.sound.AudioRouteSelectionDialogFragment";

    public AudioRouteController(Context context, String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mCarAudioManager = ((CarSettingsApplication) context.getApplicationContext())
                .getCarAudioManager();
        mAudioZone = ((CarSettingsApplication) context.getApplicationContext()).getMyAudioZoneId();
        mMediaUsage = context.getResources().getInteger(R.integer.audio_route_selector_usage);
    }

    @Override
    public boolean handlePreferenceClicked(Preference preference) {
        if (Flags.newAudioRoutingUi()) {
            getFragmentController().launchFragment(new AudioRoutePickerFragment());
        } else {
            AudioRouteSelectionDialogFragment dialog =
                            new AudioRouteSelectionDialogFragment(getContext());
            getFragmentController().showDialog(dialog, /* tag= */ null);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        getPreference().setSummary(getActiveDeviceName());
    }

    @Nullable
    private String getActiveDeviceName() {
        AudioDeviceInfo output = mCarAudioManager.getOutputDeviceForUsage(mAudioZone, mMediaUsage);
        if (output != null && output.getProductName() != null) {
            // special character % is used during string formatting for preference summaries and
            // it is not allowed to be set.
            return output.getProductName().toString().replace("%", "");
        }
        return null;
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }
}
