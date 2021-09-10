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

package com.android.car.settings.privacy;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.graphics.drawable.Drawable;
import android.hardware.SensorPrivacyManager;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.PreferenceController;
import com.android.car.ui.preference.CarUiTwoActionSwitchPreference;
import com.android.internal.annotations.VisibleForTesting;

/** Business logic for controlling the privacy center mic setting. */
public class MicPreferenceController
        extends PreferenceController<CarUiTwoActionSwitchPreference> {
    private static final Logger LOG = new Logger(MicPreferenceController.class);
    public static final String PERMISSION_GROUP_MICROPHONE = "android.permission-group.MICROPHONE";

    private SensorPrivacyManager mSensorPrivacyManager;

    private SensorPrivacyManager.OnSensorPrivacyChangedListener mListener =
            new SensorPrivacyManager.OnSensorPrivacyChangedListener() {
                @Override
                public void onSensorPrivacyChanged(int sensor, boolean enabled) {
                    refreshUi();
                }
            };

    private PackageManager mPm;

    public MicPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                SensorPrivacyManager.getInstance(context));
    }

    @VisibleForTesting
    MicPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            SensorPrivacyManager sensorPrivacyManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mSensorPrivacyManager = sensorPrivacyManager;
        mPm = context.getPackageManager();
    }

    @Override
    protected Class<CarUiTwoActionSwitchPreference> getPreferenceType() {
        return CarUiTwoActionSwitchPreference.class;
    }

    @Override
    protected void onCreateInternal() {
        try {
            PermissionGroupInfo groupInfo = mPm.getPermissionGroupInfo(
                    PERMISSION_GROUP_MICROPHONE,
                    /* flags= */ 0);
            Drawable icon = groupInfo.loadIcon(mPm);
            icon.mutate().setTintList(getContext().getColorStateList(R.color.icon_color_default));
            getPreference().setIcon(icon);
        } catch (PackageManager.NameNotFoundException e) {
            // fall back to using the CarSettings icon specified in the resource file
            LOG.e("Unable to load permission icon from PackageManager", e);
        }

        getPreference().setOnSecondaryActionClickListener(isChecked -> {
            // Settings UX currently shows "checked means mic is enabled", but the underlying API is
            // inversely written around "is mic muted?" So we must be careful when doing
            // comparisons.
            boolean isMicMuted = mSensorPrivacyManager.isSensorPrivacyEnabled(
                    SensorPrivacyManager.Sensors.MICROPHONE);
            if (isChecked == isMicMuted) {
                // UX and underlying API state for mic do not match, so update sensor privacy
                mSensorPrivacyManager.setSensorPrivacyForProfileGroup(
                        SensorPrivacyManager.Sources.SETTINGS,
                        SensorPrivacyManager.Sensors.MICROPHONE,
                        !isChecked);
            }
        });
    }

    @Override
    protected void onStartInternal() {
        mSensorPrivacyManager.addSensorPrivacyListener(
                SensorPrivacyManager.Sensors.MICROPHONE, mListener);
    }

    @Override
    protected void onStopInternal() {
        mSensorPrivacyManager.removeSensorPrivacyListener(SensorPrivacyManager.Sensors.MICROPHONE,
                mListener);
    }

    @Override
    protected int getAvailabilityStatus() {
        boolean hasFeatureMicToggle = mSensorPrivacyManager.supportsSensorToggle(
                SensorPrivacyManager.Sensors.MICROPHONE);
        return hasFeatureMicToggle ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    protected void updateState(CarUiTwoActionSwitchPreference preference) {
        preference.setSecondaryActionChecked(!mSensorPrivacyManager.isSensorPrivacyEnabled(
                SensorPrivacyManager.Sensors.MICROPHONE));
    }
}
