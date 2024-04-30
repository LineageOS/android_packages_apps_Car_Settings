/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.settings.location;

import static android.os.UserManager.DISALLOW_CONFIG_LOCATION;
import static android.os.UserManager.DISALLOW_SHARE_LOCATION;

import static com.android.car.settings.enterprise.ActionDisabledByAdminDialogFragment.DISABLED_BY_ADMIN_CONFIRM_DIALOG_TAG;
import static com.android.car.settings.enterprise.EnterpriseUtils.hasUserRestrictionByDpm;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Toast;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.enterprise.EnterpriseUtils;
import com.android.car.ui.preference.CarUiSwitchPreference;
import com.android.settingslib.Utils;

/**
 * Enables/disables location state via SwitchPreference.
 */
public class LocationStateSwitchPreferenceController extends
        LocationStateListenerBasePreferenceController<CarUiSwitchPreference> {
    private static final Logger LOG = new Logger(
            LocationStateSwitchPreferenceController.class);

    public LocationStateSwitchPreferenceController(Context context,
            String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (hasUserRestrictionByDpm(getContext(), DISALLOW_CONFIG_LOCATION)
                || hasUserRestrictionByDpm(getContext(), DISALLOW_SHARE_LOCATION)
                || !getIsPowerPolicyOn()) {
            return AVAILABLE_FOR_VIEWING;
        }
        return AVAILABLE;
    }

    @Override
    protected Class<CarUiSwitchPreference> getPreferenceType() {
        return CarUiSwitchPreference.class;
    }

    @Override
    protected void updateState(CarUiSwitchPreference preference) {
        preference.setChecked(getLocationManager().isLocationEnabled()
                && !hasUserRestrictionByDpm(getContext(), DISALLOW_SHARE_LOCATION));
    }

    @Override
    protected boolean handlePreferenceChanged(CarUiSwitchPreference preference, Object newValue) {
        boolean isLocationEnabled = (Boolean) newValue;
        Utils.updateLocationEnabled(
                getContext(),
                isLocationEnabled,
                UserHandle.myUserId(),
                Settings.Secure.LOCATION_CHANGER_SYSTEM_SETTINGS);
        return true;
    }

    @Override
    protected void onCreateInternal() {
        addDefaultMainLocationStateListener();
        addDefaultPowerPolicyListener();

        setClickableWhileDisabled(getPreference(), /* clickable= */ true, p -> {
            // All the cases here should coincide with the ones in getAvailabilityStatus()
            if (!getIsPowerPolicyOn()) {
                Toast.makeText(getContext(), R.string.power_component_disabled, Toast.LENGTH_LONG)
                        .show();
                return;
            }
            if (hasUserRestrictionByDpm(getContext(), DISALLOW_SHARE_LOCATION)) {
                showActionDisabledByAdminDialog(DISALLOW_SHARE_LOCATION);
                return;
            }
            if (hasUserRestrictionByDpm(getContext(), DISALLOW_CONFIG_LOCATION)) {
                showActionDisabledByAdminDialog(DISALLOW_CONFIG_LOCATION);
                return;
            }
        });
    }

    private void showActionDisabledByAdminDialog(String restrictionType) {
        getFragmentController().showDialog(
                EnterpriseUtils.getActionDisabledByAdminDialog(getContext(),
                        restrictionType),
                DISABLED_BY_ADMIN_CONFIRM_DIALOG_TAG);
    }
}
