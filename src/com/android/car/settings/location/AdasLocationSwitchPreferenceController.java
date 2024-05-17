/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.Flags;
import com.android.car.settings.R;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.ui.preference.CarUiSwitchPreference;

/**
 * Enables/disables ADAS (Advanced Driver-assistance systems) GNSS bypass via SwitchPreference.
 *
 * <p>This switch is not affected by {@link android.os.UserManager#DISALLOW_CONFIG_LOCATION} or
 * {@link android.os.UserManager#DISALLOW_SHARE_LOCATION} to prevent a device policy manager from
 * changing settings that can negatively impact the safety of the driver.
 */
public class AdasLocationSwitchPreferenceController extends
        LocationStateListenerBasePreferenceController<CarUiSwitchPreference> {
    private static final String AUTOMOTIVE_LOCATION_BYPASS_RESOURCE_NAME =
            "config_defaultAdasGnssLocationEnabled";
    private static final String AUTOMOTIVE_LOCATION_BYPASS_RESOURCE_TYPE = "bool";
    private static final String AUTOMOTIVE_LOCATION_BYPASS_RESOURCE_PACKAGE = "android";

    @VisibleForTesting
    boolean mIsClickable;
    @VisibleForTesting
    boolean mIsVisible;

    public AdasLocationSwitchPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<CarUiSwitchPreference> getPreferenceType() {
        return CarUiSwitchPreference.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        if (Flags.requiredInfotainmentAppsSettingsPage()) {
            if (!mIsVisible) {
                return CONDITIONALLY_UNAVAILABLE;
            }
            if (!getIsPowerPolicyOn() || getLocationManager().isLocationEnabled()) {
                return AVAILABLE_FOR_VIEWING;
            }
            return AVAILABLE;
        } else {
            return mIsClickable && getIsPowerPolicyOn() && !getLocationManager().isLocationEnabled()
                    ? AVAILABLE
                    : AVAILABLE_FOR_VIEWING;
        }
    }

    @Override
    protected void updateState(CarUiSwitchPreference preference) {
        preference.setChecked(getLocationManager().isAdasGnssLocationEnabled());
    }

    @Override
    protected boolean handlePreferenceChanged(CarUiSwitchPreference preference, Object newValue) {
        if (Flags.requiredInfotainmentAppsSettingsPage()) {
            getLocationManager().setAdasGnssLocationEnabled((Boolean) newValue);
            return true;
        } else {
            if (getLocationManager().isAdasGnssLocationEnabled()) {
                getFragmentController().showDialog(getConfirmationDialog(),
                        ConfirmationDialogFragment.TAG);
                return false;
            } else {
                getLocationManager().setAdasGnssLocationEnabled((Boolean) newValue);
            }
            return true;
        }
    }

    @Override
    protected void onCreateInternal() {
        addDefaultBypassLocationStateListener();
        setMainLocationStateListener((isEnabled) -> {
            // Turns ADAS location on when main location switch is on. Location service do
            // not support the case where main location switch on and ADAS location off.
            if (isEnabled) {
                getLocationManager().setAdasGnssLocationEnabled(true);
            }
        });
        addDefaultPowerPolicyListener();

        if (Flags.requiredInfotainmentAppsSettingsPage()) {
            Resources res = Resources.getSystem();
            int resId = res.getIdentifier(
                    AUTOMOTIVE_LOCATION_BYPASS_RESOURCE_NAME,
                    AUTOMOTIVE_LOCATION_BYPASS_RESOURCE_TYPE,
                    AUTOMOTIVE_LOCATION_BYPASS_RESOURCE_PACKAGE);
            boolean defaultLocationBypassEnabled = res.getBoolean(resId);
            // If by default automotive location bypass is on, then
            // config_show_location_required_apps_toggle will dictate the visibility.
            // Otherwise the toggle must be visible.
            mIsVisible = defaultLocationBypassEnabled
                    ? getContext().getResources().getBoolean(
                            R.bool.config_show_location_required_apps_toggle)
                    : true;
        } else {
            mIsClickable = getContext().getResources()
                    .getBoolean(R.bool.config_allow_adas_location_switch_clickable);
        }

        setClickableWhileDisabled(getPreference(), /* clickable= */true, preference -> {
            if (Flags.requiredInfotainmentAppsSettingsPage()) {
                if (!getIsPowerPolicyOn()) {
                    Toast.makeText(getContext(), R.string.power_component_disabled,
                            Toast.LENGTH_LONG).show();
                }
            } else {
                if (!mIsClickable) {
                    getFragmentController().showDialog(getToggleDisabledDialog(),
                            ConfirmationDialogFragment.TAG);
                    return;
                }
                if (!getIsPowerPolicyOn()) {
                    Toast.makeText(getContext(), R.string.power_component_disabled,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Assigns confirm action as negative button listener and cancel action as positive button
     * listener, because the UX design requires the cancel button has to be on right and the confirm
     * button on left.
     */
    private ConfirmationDialogFragment getConfirmationDialog() {
        return new ConfirmationDialogFragment.Builder(getContext())
                .setMessage(R.string.adas_location_toggle_off_warning)
                .setNegativeButton(
                        R.string.adas_location_toggle_confirm_label,
                        arguments -> {
                            // This if statement is included because the power policy handler runs
                            // slightly after the UI is initialized. Therefore, there's a small
                            // timeframe for the user to toggle the switch before the UI updates
                            // and disables the switch because the power policy is off. This if
                            // statement mitigates this issue by reverifying the power policy
                            // status.
                            if (getIsPowerPolicyOn()) {
                                getLocationManager().setAdasGnssLocationEnabled(false);
                            }
                        })
                .setPositiveButton(android.R.string.cancel, /* confirmListener= */ null)
                .build();
    }

    private ConfirmationDialogFragment getToggleDisabledDialog() {
        return new ConfirmationDialogFragment.Builder(getContext())
                .setMessage(R.string.adas_location_toggle_popup_summary)
                .setPositiveButton(android.R.string.ok, /* confirmListener= */ null)
                .build();
    }
}
