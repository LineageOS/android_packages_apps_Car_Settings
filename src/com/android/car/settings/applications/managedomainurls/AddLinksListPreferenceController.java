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

import static android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_NONE;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.ui.preference.CarUiMultiSelectListPreference;
import com.android.settingslib.utils.StringUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A controller for adding links that can be opened by the app.
 */
public class AddLinksListPreferenceController  extends
        AppLaunchSettingsBasePreferenceController<CarUiMultiSelectListPreference> {
    private static final Logger LOG = new Logger(AddLinksListPreferenceController.class);

    public AddLinksListPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<CarUiMultiSelectListPreference> getPreferenceType() {
        return CarUiMultiSelectListPreference.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (isLinkHandlingEnabled() && getLinksNumber(DOMAIN_STATE_NONE) > 0) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected void updateState(CarUiMultiSelectListPreference preference) {
        super.updateState(preference);
        updatePreferenceOptions();
    }

    @Override
    protected boolean handlePreferenceChanged(CarUiMultiSelectListPreference preference,
            Object newValue) {
        Set<String> selectedEntries = (Set<String>) newValue;
        handleAddedLinks(selectedEntries);
        return true;
    }

    private void handleAddedLinks(Set<String> selectedEntries) {
        if (selectedEntries.size() == 0) {
            return;
        }
        getDomainVerificationManager().setDomainVerificationUserSelection(getPackageName(),
                selectedEntries, true);
        refreshUi();
    }

    private void updatePreferenceOptions() {
        List<String> entries = getDomainVerificationManager().getLinksList(getPackageName(),
                DOMAIN_STATE_NONE);

        getPreference().setEntries(entries.toArray(new CharSequence[entries.size()]));
        getPreference().setEntryValues(entries.toArray(new CharSequence[entries.size()]));
        getPreference().setValues(new HashSet<>());
        getPreference().setSummary(
                StringUtil.getIcuPluralsString(getContext(), getLinksNumber(DOMAIN_STATE_NONE),
                        R.string.opening_links_add_links_summary));
    }
}
