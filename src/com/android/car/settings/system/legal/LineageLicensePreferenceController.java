/*
 * Copyright (C) 2024 The LineageOS Project
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

package com.android.car.settings.system.legal;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;

public class LineageLicensePreferenceController extends LegalPreferenceController {

    private static final String PROPERTY_LINEAGE_LICENSE_URL = "ro.lineagelegal.url";

    public LineageLicensePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected void updateState(Preference preference) {
        preference.setIntent(getIntent());
    }

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse(SystemProperties.get(PROPERTY_LINEAGE_LICENSE_URL)));
    }
}
