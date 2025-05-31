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

package com.android.car.settings.bluetooth.audiosharing.audiostreaming;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import com.android.car.settings.bluetooth.audiosharing.BaseAudioSharingPreferenceController;
import com.android.car.settings.common.FragmentController;
import com.android.car.ui.preference.CarUiTwoActionIconPreference;

/**
 * Controller for launching {@link AudioStreamFragment}.
 */
public class AudioStreamController extends
        BaseAudioSharingPreferenceController<CarUiTwoActionIconPreference> {

    public AudioStreamController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected void updateState(CarUiTwoActionIconPreference preference) {
        super.updateState(preference);
        preference.setOnSecondaryActionClickListener(() -> {
            getFragmentController().launchFragment(new AudioStreamFragment());
        });
        preference.setSecondaryActionVisible(true);
    }

    @Override
    protected Class<CarUiTwoActionIconPreference> getPreferenceType() {
        return CarUiTwoActionIconPreference.class;
    }
}
