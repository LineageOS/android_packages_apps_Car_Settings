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

import static android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_VERIFIED;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.preference.PreferenceGroup;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.car.ui.preference.CarUiPreference;

import java.util.List;

/** A {@link PreferenceController} to show a verified link list. */
public class VerifiedLinkGroupPreferenceController extends
        AppLaunchSettingsBasePreferenceController<PreferenceGroup> {

    public VerifiedLinkGroupPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void onStartInternal() {
        super.onStartInternal();
        showLinksList();
    }

    private void showLinksList() {
        getPreference().removeAll();

        List<String> linksList = getDomainVerificationManager().getLinksList(getPackageName(),
                DOMAIN_STATE_VERIFIED);
        for (String link : linksList) {
            CarUiPreference carUiPreference = new CarUiPreference(getContext());
            carUiPreference.setTitle(link);
            carUiPreference.setKey(link);
            carUiPreference.setSelectable(false);
            getPreference().addPreference(carUiPreference);
        }
    }
}
