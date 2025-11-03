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
import android.util.AttributeSet;

import com.android.car.settings.R;
import com.android.car.settings.common.CollapsibleSeekbarPreference;

/**
 * A preference class for audio routing that extends {@link CollapsibleSeekbarPreference}. This
 * class encapsulates the UI logic.
 */
public class AudioRoutePreference extends CollapsibleSeekbarPreference {

    public AudioRoutePreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AudioRoutePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AudioRoutePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AudioRoutePreference(Context context) {
        super(context);
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
