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

import static android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;

import com.android.car.settings.common.FragmentController;
import com.android.car.ui.preference.CarUiPreference;

/**
 * Preference controller that plays the default notification chime on the media channel.
 */
public class PlaySoundPreferenceController extends
        BaseAudioSharingPreferenceController<CarUiPreference> {
    private final Ringtone mRingtone;

    public PlaySoundPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mRingtone = RingtoneManager.getRingtone(context, DEFAULT_NOTIFICATION_URI);
        mRingtone.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
    }

    @Override
    protected void onStartInternal() {
        super.onStartInternal();
        getPreference().setOnPreferenceClickListener(pref -> {
            if (mRingtone != null) {
                mRingtone.stop();
                mRingtone.play();
            }
            return true;
        });
    }

    @Override
    protected void onPauseInternal() {
        super.onResumeInternal();
        if (mRingtone != null) {
            mRingtone.stop();
        }
    }

    @Override
    protected Class<CarUiPreference> getPreferenceType() {
        return CarUiPreference.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (isUserEnabled() && isBroadcasting()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }}
