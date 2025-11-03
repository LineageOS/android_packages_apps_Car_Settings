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

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.sound.AudioRoutePreference;
import com.android.car.ui.R;

public class AudioRoutePreferenceGroup extends PreferenceGroup {

    public AudioRoutePreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.audio_route_preference_group);
    }

    public AudioRoutePreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AudioRoutePreferenceGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioRoutePreferenceGroup(Context context) {
        this(context, null);
    }

    @NonNull
    @Override
    public AudioRoutePreference getPreference(int index) {
        return (AudioRoutePreference) super.getPreference(index);
    }
}
