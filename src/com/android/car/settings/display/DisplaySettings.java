/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.car.settings.display;

import android.content.Context;
import android.os.Bundle;
import com.android.car.settings.R;

import com.android.car.settings.core.CarPreferenceController;
import com.android.car.settings.core.CarSettings;
import java.util.ArrayList;
import java.util.List;


/**
 * DisplaySettingsActivity fragments for display related settings.
 */
public class DisplaySettings extends CarSettings {
    private static final String TAG = "DisplaySettings";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.display_settings);
        super.onCreatePreferences(savedInstanceState, rootKey);
    }

    @Override
    public List<CarPreferenceController> getControllers(Context context) {
        List<CarPreferenceController> controllers =
            new ArrayList<CarPreferenceController>();
        controllers.add(new AutoBrightnessPreferenceController(context));
        controllers.add(new BrightnessPreferenceController(context));
        return controllers;
    }
}
