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

package com.android.car.settings.sound;

import android.content.Context;

import com.android.car.settings.R;
import com.android.car.settings.common.CollapsibleSeekbarPreference;
import com.android.car.settings.common.Logger;

/**
 * A preference class for audio routing that extends {@link CollapsibleSeekbarPreference}.
 *
 * <p>This class encapsulates the UI logic for displaying and interacting with an individual audio
 * route. It updates its appearance (e.g., title, summary, seeker bar) based on the state of an
 * {@link AudioRouteItem}.
 */
public class AudioRoutePreference extends CollapsibleSeekbarPreference {
    private static final Logger LOG = new Logger(AudioRoutePreference.class);

    /**
     * Constructs a new AudioRoutePreference.
     *
     * @param context The context this preference is running in.
     * @param item    The initial {@link AudioRouteItem} to populate the preference's UI.
     */
    public AudioRoutePreference(Context context, AudioRouteItem item) {
        super(context);
        setPersistent(false);
        updateUi(item);
    }

    public AudioRoutePreference(Context context) {
        super(context);
    }

    /**
     * Updates the UI of the preference based on the state of the given {@link AudioRouteItem}.
     *
     * <p>This method adjusts the title, summary, seeker bar visibility, and other UI elements
     * to reflect the current status of the audio route (e.g., connecting, active, available to
     * join).
     *
     * @param item The {@link AudioRouteItem} containing the state and properties to update the UI
     *             with.
     */
    public void updateUi(AudioRouteItem item) {
        setTitle(item.getName());
        setKey(item.getAddress());
        boolean showAudioSharing =
                item.getGlobalState().isAudioSharingEnabled() && item.isBluetoothAudioRoute();
        setMultiSelectAvailable(showAudioSharing);

        switch (item.getState()) {
            case UNICAST_READY, BROADCAST_READY:
                updateState(STATE_UNSELECTED, /* showActionButton= */ false);
                setSummary(null);
                break;
            case STARTING_UNICAST:
                updateState(STATE_UNSELECTED, /* showActionButton= */ false);
                setSummary(R.string.audio_route_preference_connecting_device);
                break;
            case UNICAST_ACTIVE:
                updateState(STATE_SELECTED, /* showActionButton= */ false);
                setSummary(R.string.audio_route_preference_listening);
                break;
            case MULTICAST_READY_UNICAST_READY:
                updateState(STATE_UNSELECTED, /* showActionButton= */ true);
                setSummary(R.string.audio_route_preference_available_to_join);
                break;
            case MULTICAST_READY_UNICAST_ACTIVE:
                break;
            case STARTING_BROADCAST:
                updateState(item.getAudioZoneConfigState().isSelected() ? STATE_SELECTED
                        : STATE_UNSELECTED, /* showActionButton= */ true);
                setSummary(R.string.audio_route_preference_starting_broadcast);
                break;
            case JOINING_BROADCAST:
                updateState(item.getAudioZoneConfigState().isSelected() ? STATE_SELECTED
                        : STATE_UNSELECTED, /* showActionButton= */ true);
                setSummary(R.string.audio_route_preference_connecting_broadcast);
                break;
            case MULTICAST_ACTIVE:
                updateState(STATE_MULTI_SELECTED,  /* showActionButton= */ true);
                setSummary(R.string.audio_route_preference_listening);
                break;
            case LEAVING_BROADCAST:
                updateState(STATE_MULTI_SELECTED, /* showActionButton= */ false);
                setSummary(R.string.audio_route_preference_leaving_broadcast);
                break;
            case BROADCAST_ACTIVE:
                updateState(STATE_SELECTED, /* showActionButton= */ false);
                setSummary(null);
                break;
            default:
                break;
        }
    }

    /**
     * Sets the preference to a unicast active state.
     *
     * @param isLeCapable true if the device is LE capable, false otherwise.
     */
    public void setUnicastActive(boolean isLeCapable) {
        setSummary(R.string.audio_route_preference_listening);
        updateState(STATE_SELECTED, isLeCapable);
    }

    /** Sets the preference to a unicast inactive state. */
    public void setUnicastInactive() {
        setSummary(null);
        updateState(STATE_UNSELECTED, false);
    }

    /** Sets the preference to a multicast active state. */
    public void setMulticastActive() {
        setSummary(R.string.audio_route_preference_listening);
        updateState(STATE_MULTI_SELECTED, true);
    }

    /** Sets the preference to a multicast inactive state. */
    public void setMulticastInactive() {
        setSummary(R.string.audio_route_preference_available_to_join);
        updateState(STATE_UNSELECTED, true);
    }
}
