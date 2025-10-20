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
import com.android.car.settings.common.SettingsFragment;

/**
 * Preference screen that displays and allows users to choose the sound output channel.
 */
public class AudioRoutePickerFragment extends SettingsFragment {

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.audio_route_picker_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(AudioRouteSelectorController.class, R.string.pk_audio_route_preference_group);
    }
}
