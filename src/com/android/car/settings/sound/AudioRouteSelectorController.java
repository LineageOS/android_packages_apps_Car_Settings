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

package com.android.car.settings.sound;

import static com.android.car.settings.common.CollapsibleSeekbarPreference.STATE_MULTI_SELECTED;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.Flags;
import com.android.car.settings.R;
import com.android.car.settings.common.CollapsibleSeekbarPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.sound.audiorouting.AudioRoutePreferenceGroup;

import java.util.List;

/**
 * Controls the audio destination selection.
 */
public class AudioRouteSelectorController extends PreferenceController<AudioRoutePreferenceGroup>
        implements CollapsibleSeekbarPreference.CollapsibleSeekbarUpdateListener {
    private static final Logger LOG = new Logger(AudioRouteSelectorController.class);
    private AudioRoutesManager mRouteManager;

    AudioRoutesManager.AudioRoutesUpdateListener mAudioRoutesUpdateListener =
            audioRouteItems -> {
                LOG.d("onAudioRoutesUpdated: " + audioRouteItems);
                updatePreferenceOptions(audioRouteItems);
            };

    public AudioRouteSelectorController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mRouteManager = new AudioRoutesManager(context,
                context.getResources().getInteger(R.integer.audio_route_selector_usage));
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        mRouteManager.setAudioRoutesUpdateListener(mAudioRoutesUpdateListener);
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (Flags.newAudioRoutingUi()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected Class<AudioRoutePreferenceGroup> getPreferenceType() {
        return AudioRoutePreferenceGroup.class;
    }

    private void updatePreferenceOptions(List<AudioRouteItem> audioRouteItems) {
        if (getAvailabilityStatus() == CONDITIONALLY_UNAVAILABLE
                || !mRouteManager.isAudioRoutingEnabled()) {
            return;
        }
        LOG.d("Updating preferences options based on updated audio routes");
        getPreference().removeAll();
        for (AudioRouteItem item : audioRouteItems) {
            // Broadcast is not displayed as a possible audio route.
            if (item.getState().isBroadcastState()) continue;
            AudioRoutePreference pref = new AudioRoutePreference(getContext(), item);
            pref.setListener(this);
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                int volume = (Integer) newValue;
                mRouteManager.setVolume(item.getAddress(), volume);
                return true;
            });
            getPreference().addPreference(pref);
        }
    }

    @Override
    protected void onDestroyInternal() {
        mRouteManager.tearDown();
    }

    @Override
    public void onSelected(String key, int currentState) {
        mRouteManager.setUnicast(key);
    }

    @Override
    public void onMultiSelected(String key, int currentState) {
        if (currentState == STATE_MULTI_SELECTED) {
            mRouteManager.leaveBroadcast(/* leavingAddress= */ key);
        } else {
            mRouteManager.joinBroadcast(/* joiningAddress= */ key);
        }
    }

    @VisibleForTesting
    void setAudioRoutesManager(AudioRoutesManager audioRoutesManager) {
        mRouteManager = audioRoutesManager;
    }
}
