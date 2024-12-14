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

package com.android.car.settings.applications.managedomainurls;

import static android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_SELECTED;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.ui.preference.CarUiMultiSelectListPreference;
import com.android.settingslib.utils.StringUtil;

/**
 * Controls the SelectedLinksList preference.
 */
public class SelectedLinksListPreferenceController extends
        AppLaunchSettingsBasePreferenceController<CarUiMultiSelectListPreference> {
    private static final Logger LOG = new Logger(SelectedLinksListPreferenceController.class);

    public SelectedLinksListPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<CarUiMultiSelectListPreference> getPreferenceType() {
        return CarUiMultiSelectListPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        // TODO: add entries in part 2 CL.
        getPreference().setSelectable(false);
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (isLinkHandlingEnabled()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected void updateState(CarUiMultiSelectListPreference preference) {
        super.updateState(preference);
        preference.setSummary(
                StringUtil.getIcuPluralsString(getContext(), getLinksNumber(DOMAIN_STATE_SELECTED),
                        R.string.opening_links_supported_links_summary));
    }
}
