/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.common;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects preferences from other system applications at a placeholder location. The placeholder
 * should be a {@link Preference} which sets the controller attribute to this name. If the order
 * attribute is set, it will be applied to the extra settings in the category. The preference
 * should contain an intent which will be passed to
 * {@link ExtraSettingsLoader#loadPreferences(Intent)}.
 *
 * <p>For example:
 * <pre>{@code
 * <Preference
 *     android:key="system_extra_settings"
 *     android:order="100"
 *     settings:controller="com.android.settings.common.ExtraSettingsPreferenceController">
 *     <intent android:action="com.android.settings.action.EXTRA_SETTINGS">
 *         <extra android:name="com.android.settings.category"
 *                android:value="com.android.settings.category.system"/>
 *     </intent>
 * </Preference>
 * }</pre>
 *
 * @see ExtraSettingsLoader
 */
// TODO: investigate using SettingsLib Tiles.
public class ExtraSettingsPreferenceController extends NoSetupPreferenceController {

    private final ExtraSettingsLoader mExtraSettingsLoader;
    private final List<Preference> mExtraSettings = new ArrayList<>();

    public ExtraSettingsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExtraSettingsLoader = new ExtraSettingsLoader(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        Preference placeholder = screen.findPreference(getPreferenceKey());
        if (placeholder != null) {
            mExtraSettings.clear();
            int order = placeholder.getOrder();
            screen.removePreference(placeholder);
            List<Preference> extraSettings = mExtraSettingsLoader.loadPreferences(
                    placeholder.getIntent());
            for (Preference setting : extraSettings) {
                setting.setVisible(isAvailable());
                setting.setOrder(order);
                screen.addPreference(setting);
                mExtraSettings.add(setting);
            }
        } else {
            for (Preference pref : mExtraSettings) {
                pref.setVisible(isAvailable());
            }
        }
    }
}
