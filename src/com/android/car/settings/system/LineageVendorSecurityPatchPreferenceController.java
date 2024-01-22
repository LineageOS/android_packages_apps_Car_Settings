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

package com.android.car.settings.system;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.SystemProperties;
import android.text.format.DateFormat;

import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LineageVendorSecurityPatchPreferenceController
        extends PreferenceController<Preference> {

    private static final String KEY_AOSP_VENDOR_SECURITY_PATCH =
            "ro.vendor.build.security_patch";

    private static final String KEY_LINEAGE_VENDOR_SECURITY_PATCH =
            "ro.lineage.build.vendor_security_patch";

    public LineageVendorSecurityPatchPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    protected void updateState(Preference preference) {
        String patchLevel = SystemProperties.get(KEY_AOSP_VENDOR_SECURITY_PATCH);

        if (patchLevel.isEmpty()) {
            patchLevel = SystemProperties.get(KEY_LINEAGE_VENDOR_SECURITY_PATCH);
        }

        if (!patchLevel.isEmpty()) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchLevelDate = template.parse(patchLevel);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                patchLevel = DateFormat.format(format, patchLevelDate).toString();
            } catch (ParseException e) {
                // parsing failed, use raw string
            }
        } else {
            patchLevel = getContext().getString(R.string.unknown);
        }

        preference.setSummary(patchLevel);
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        return AVAILABLE_FOR_VIEWING;
    }
}
