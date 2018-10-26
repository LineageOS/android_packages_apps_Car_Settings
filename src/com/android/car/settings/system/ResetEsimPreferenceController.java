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

package com.android.car.settings.system;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.telephony.euicc.EuiccManager;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;

/**
 * Controller which determines if a checkbox to reset the device's eSIMs is shown. Not all
 * devices support eSIMs.
 */
public class ResetEsimPreferenceController extends NoSetupPreferenceController {

    public ResetEsimPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    @Override
    public int getAvailabilityStatus() {
        return showEuiccSettings() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean showEuiccSettings() {
        EuiccManager euiccManager = (EuiccManager) mContext.getSystemService(Context.EUICC_SERVICE);
        if (!euiccManager.isEnabled()) {
            return false;
        }
        ContentResolver resolver = mContext.getContentResolver();
        return Settings.Global.getInt(resolver, Settings.Global.EUICC_PROVISIONED, 0) != 0
                || Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
                != 0;
    }
}
