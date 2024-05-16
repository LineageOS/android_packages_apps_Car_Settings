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

package com.android.car.settings.privacy;

import static com.android.car.settings.enterprise.EnterpriseUtils.getAvailabilityStatusRestricted;
import static com.android.car.settings.enterprise.EnterpriseUtils.hasUserRestrictionByDpm;
import static com.android.car.settings.enterprise.EnterpriseUtils.onClickWhileDisabled;

import android.annotation.FlaggedApi;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.os.UserManager;

import com.android.car.settings.common.FragmentController;
import com.android.car.ui.preference.CarUiSwitchPreference;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.camera.flags.Flags;

/** Business logic for controlling the mute camera toggle. */
@FlaggedApi(Flags.FLAG_CAMERA_PRIVACY_ALLOWLIST)
public class CameraInfotainmentAppsTogglePreferenceController
        extends CameraPrivacyBasePreferenceController<CarUiSwitchPreference> {
    private boolean mHasCameraPrivacyAllowlist = false;

    public CameraInfotainmentAppsTogglePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                SensorPrivacyManager.getInstance(context));
    }

    @VisibleForTesting
    CameraInfotainmentAppsTogglePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            SensorPrivacyManager sensorPrivacyManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions, sensorPrivacyManager);
        mHasCameraPrivacyAllowlist = !sensorPrivacyManager.getCameraPrivacyAllowlist().isEmpty();
    }

    @Override
    protected Class<CarUiSwitchPreference> getPreferenceType() {
        return CarUiSwitchPreference.class;
    }

    private boolean isCameraMutedForInfotainmentApps() {
        int state = getSensorPrivacyManager().getSensorPrivacyState(
                SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE, SensorPrivacyManager.Sensors.CAMERA);
        return (state != SensorPrivacyManager.StateTypes.DISABLED);
    }

    @Override
    protected boolean handlePreferenceChanged(CarUiSwitchPreference preference,
            Object newValue) {
        boolean isChecked = (Boolean) newValue;
        if (isChecked == isCameraMutedForInfotainmentApps()) {
            int newState;
            if (isChecked) {
                newState =  SensorPrivacyManager.StateTypes.DISABLED;
            } else {
                newState = mHasCameraPrivacyAllowlist
                        ? SensorPrivacyManager.StateTypes.ENABLED_EXCEPT_ALLOWLISTED_APPS
                        : SensorPrivacyManager.StateTypes.ENABLED;
            }

            getSensorPrivacyManager().setSensorPrivacyStateForProfileGroup(
                    SensorPrivacyManager.Sources.SETTINGS,
                    SensorPrivacyManager.Sensors.CAMERA,
                    newState);
        }
        return true;
    }

    @Override
    protected void updateState(CarUiSwitchPreference preference) {
        preference.setChecked(!isCameraMutedForInfotainmentApps());
        if (hasUserRestrictionByDpm(getContext(), UserManager.DISALLOW_CAMERA_TOGGLE)) {
            setClickableWhileDisabled(preference, /* clickable= */ true, p ->
                    onClickWhileDisabled(getContext(), getFragmentController(),
                            UserManager.DISALLOW_CAMERA_TOGGLE));
        }
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        return getAvailabilityStatusRestricted(getContext(), UserManager.DISALLOW_CAMERA_TOGGLE);
    }
}
