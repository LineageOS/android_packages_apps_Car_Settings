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

import static com.android.car.ui.preference.CarUiTwoActionTextPreference.SECONDARY_ACTION_STYLE_BORDERLESS;

import android.Manifest;
import android.annotation.FlaggedApi;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.SensorPrivacyManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.util.IconDrawableFactory;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceController;
import com.android.car.ui.preference.CarUiTwoActionTextPreference;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.camera.flags.Flags;

import java.util.List;

/**
 * Displays a list of apps which are required for driving with their privacy policy and a
 * link to their camera permission settings.
 */
@FlaggedApi(Flags.FLAG_CAMERA_PRIVACY_ALLOWLIST)
public final class CameraRequiredAppsPreferenceController
        extends PreferenceController<LogicalPreferenceGroup> {

    private static final Logger LOG = new Logger(CameraRequiredAppsPreferenceController.class);
    private static final String PRIVACY_POLICY_KEY = "privacy_policy";
    private final PackageManager mPackageManager;
    private final SensorPrivacyManager mSensorPrivacyManager;
    private List<String> mCameraPrivacyAllowlist;

    public CameraRequiredAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                context.getPackageManager(), SensorPrivacyManager.getInstance(context));
    }

    @VisibleForTesting
    CameraRequiredAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            PackageManager packageManager, SensorPrivacyManager sensorPrivacyManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mPackageManager = packageManager;
        mSensorPrivacyManager = sensorPrivacyManager;
        mCameraPrivacyAllowlist = mSensorPrivacyManager.getCameraPrivacyAllowlist();
    }

    @Override
    protected Class<LogicalPreferenceGroup> getPreferenceType() {
        return LogicalPreferenceGroup.class;
    }

    @Override
    protected int getDefaultAvailabilityStatus() {
        boolean hasRequiredApps = !mCameraPrivacyAllowlist.isEmpty();
        return hasRequiredApps ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    protected void onCreateInternal() {
        super.onCreateInternal();
        loadCameraRequiredAppsWithCameraPermission();
    }

    private void loadCameraRequiredAppsWithCameraPermission() {
        LogicalPreferenceGroup requiredappsPref = getPreference().findPreference(getContext()
                .getString(R.string.pk_camera_required_apps_list));

        for (String app : mCameraPrivacyAllowlist) {
            CarUiTwoActionTextPreference preference = createRequiredAppPreference(app);
            if (preference != null) {
                requiredappsPref.addPreference(preference);
            }
        }
    }

    private CarUiTwoActionTextPreference createRequiredAppPreference(String pkgName) {
        CarUiTwoActionTextPreference pref =
                new CarUiTwoActionTextPreference(getContext(), SECONDARY_ACTION_STYLE_BORDERLESS);

        IconDrawableFactory drawableFactory = IconDrawableFactory.newInstance(getContext());
        UserHandle userHandle = Process.myUserHandle();
        int userId = userHandle.getIdentifier();

        ApplicationInfo appInfo;
        PackageInfo packageInfo;
        try {
            appInfo = mPackageManager.getApplicationInfoAsUser(pkgName,
                    PackageManager.GET_META_DATA, userId);
            packageInfo = mPackageManager.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException ex) {
            LOG.e("Failed to get application info for " + pkgName);
            return null;
        }

        pref.setIcon(drawableFactory.getBadgedIcon(appInfo, userId));

        CharSequence appLabel = mPackageManager.getApplicationLabel(appInfo);
        CharSequence badgedAppLabel = mPackageManager.getUserBadgedLabel(appLabel, userHandle);
        pref.setTitle(badgedAppLabel);
        Integer permGrantStatus = PermissionUtils.getPermissionGrantStatus(getContext(),
                packageInfo, PermissionUtils.CAMERA, userHandle);
        pref.setSummary(permGrantStatus);

        pref.setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSION);
                    intent.putExtra(
                            Intent.EXTRA_PERMISSION_GROUP_NAME, Manifest.permission_group.CAMERA);
                    intent.putExtra(Intent.EXTRA_PACKAGE_NAME, pkgName);
                    intent.putExtra(Intent.EXTRA_USER, userHandle);
                    getContext().startActivity(intent);
                    return true;
                });

        pref.setSecondaryActionText(R.string.camera_privacy_policy_button_text);

        Bundle bundle = appInfo.metaData;

        if (bundle == null) {
            LOG.e(pkgName + "doesn't provide meta data in manifest");
            return pref;
        }
        CharSequence privacyPolicyLink = bundle.getCharSequence(PRIVACY_POLICY_KEY);
        if (privacyPolicyLink == null) {
            LOG.e(pkgName + " doesn't provide privacy policy");
            return pref;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyLink.toString()));

        pref.setOnSecondaryActionClickListener(
                () -> {
                    getContext().startActivity(intent);
                });
        return pref;
    }
}
