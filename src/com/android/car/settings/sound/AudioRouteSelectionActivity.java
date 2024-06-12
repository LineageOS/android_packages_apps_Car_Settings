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

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.android.car.settings.common.Logger;

/**
 * An activity hosting {@link AudioRouteSelectionDialogFragment}.
 */
public class AudioRouteSelectionActivity extends FragmentActivity {
    private static final Logger LOG = new Logger(AudioRouteSelectionActivity.class);
    public static final String FRAGMENT_TAG = "sound.audio.selection.fragment";
    public static final String INTENT_ACTION = "com.android.car.settings.AUDIO_ROUTE_SETTINGS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if the fragment has been preloaded
        AudioRouteSelectionDialogFragment dialogFragment =
                (AudioRouteSelectionDialogFragment) getSupportFragmentManager().findFragmentByTag(
                        FRAGMENT_TAG);
        // dismiss the fragment if it is already used
        if (dialogFragment != null) {
            dialogFragment.dismiss();
            dialogFragment = null;
        }

        dialogFragment = new AudioRouteSelectionDialogFragment(getApplicationContext());
        dialogFragment.show(getSupportFragmentManager(), FRAGMENT_TAG);
    }
}
