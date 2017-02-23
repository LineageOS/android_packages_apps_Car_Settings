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
package com.android.car.settings.core;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.car.settings.R;
import com.android.car.settings.display.AutoBrightnessPreferenceController;
import java.util.ArrayList;
import java.util.List;


/**
 * Settings fragments for car related settings.
 */
public abstract class CarSettings extends PreferenceFragment {
    private static final String TAG = "CarSettings";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        List<CarPreferenceController> controllers = getControllers(getContext());
        for (CarPreferenceController controller : controllers) {
            bindController(controller);
        }
    }

    abstract public List<CarPreferenceController> getControllers(Context context);

    private void bindController(@NonNull CarPreferenceController controller) {
        Preference preference = findPreference(controller.getPreferenceKey());
        if (preference == null) {
            Log.w(TAG, String.format(
                "Preference[%s] does not exist.", controller.getPreferenceKey()));
            return;
        }
        preference.setOnPreferenceChangeListener(controller);
        controller.updateState(preference);
    }
}
