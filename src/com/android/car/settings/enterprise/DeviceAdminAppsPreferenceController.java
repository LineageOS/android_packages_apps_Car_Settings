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

package com.android.car.settings.enterprise;

import android.annotation.UserIdInt;
import android.app.AppGlobals;
import android.app.admin.DeviceAdminInfo;
import android.car.drivingstate.CarUxRestrictions;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserHandle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.common.FragmentController;
import com.android.car.ui.preference.CarUiTwoActionSwitchPreference;
import com.android.internal.annotations.VisibleForTesting;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

/**
 * Displays a list of device admin apps and provides toggles to allow the user to grant/revoke
 * permission.
 * <p>Before changing the value of a permission, the user is directed to a confirmation
 * screen with more detailed information about the risks and potential effects.
 */
public class DeviceAdminAppsPreferenceController extends
        BaseDeviceAdminAddPreferenceController<PreferenceGroup> {

    private static final int PACKAGE_FLAGS = PackageManager.GET_META_DATA
            | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
            | PackageManager.MATCH_DIRECT_BOOT_UNAWARE
            | PackageManager.MATCH_DIRECT_BOOT_AWARE;

    private final IPackageManager mIPackageManager;

    public DeviceAdminAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions,
                AppGlobals.getPackageManager());
    }

    @VisibleForTesting
    DeviceAdminAppsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            IPackageManager iPackageManager) {
        super(context, preferenceKey, fragmentController, uxRestrictions);

        mIPackageManager = iPackageManager;
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void updateState(PreferenceGroup preferenceGroup) {
        preferenceGroup.removeAll();

        // TODO(b/197540650) Handle the case that there is no device admin apps.
        List<UserHandle> profiles = mUm.getUserProfiles();
        for (UserHandle profile : profiles) {
            int profileId = profile.getIdentifier();
            List<ComponentName> activeAdmins = mDpm.getActiveAdminsAsUser(profileId);
            if (activeAdmins == null) {
                return;
            }

            for (ComponentName activeAdmin : activeAdmins) {
                ActivityInfo ai;
                try {
                    ai = mIPackageManager.getReceiverInfo(activeAdmin, PACKAGE_FLAGS, profileId);
                } catch (RemoteException e) {
                    mLogger.w("Unable to load component: " + activeAdmin);
                    continue;
                }
                DeviceAdminInfo deviceAdminInfo = createDeviceAdminInfo(ai);
                if (deviceAdminInfo == null) {
                    continue;
                }
                preferenceGroup.addPreference(createPreference(deviceAdminInfo));
            }
        }
    }

    private Preference createPreference(DeviceAdminInfo deviceAdminInfo) {
        int uid = getUserIdFromDeviceAdminInfo(deviceAdminInfo);
        CarUiTwoActionSwitchPreference preference =
                new CarUiTwoActionSwitchPreference(getContext());
        preference.setTitle(deviceAdminInfo.loadLabel(mPm));
        preference.setIcon(
                mPm.getUserBadgedIcon(deviceAdminInfo.loadIcon(mPm), new UserHandle(uid)));
        preference.setKey(deviceAdminInfo.getPackageName());
        boolean isEnabled = isEnabled(deviceAdminInfo.getComponent(), uid);
        // TODO(b/185182679): Show different screen depending on the current value of isEnabled.
        Fragment fragmentOnClick = new DeviceAdminAddFragment();
        preference.setOnPreferenceClickListener(p -> {
            getFragmentController().launchFragment(fragmentOnClick);
            return true;
        });
        preference.setSecondaryActionChecked(isEnabled);
        preference.setOnSecondaryActionClickListener(
                v -> getFragmentController().launchFragment(fragmentOnClick));

        return preference;
    }

    private boolean isEnabled(ComponentName componentName, int uid) {
        return !mDpm.isRemovingAdmin(componentName, uid);
    }

    private @Nullable DeviceAdminInfo createDeviceAdminInfo(ActivityInfo ai) {
        try {
            return new DeviceAdminInfo(getContext(), ai);
        } catch (XmlPullParserException | IOException e) {
            mLogger.w("Skipping " + ai, e);
        }
        return null;
    }

    private static @UserIdInt int getUserIdFromDeviceAdminInfo(DeviceAdminInfo adminInfo) {
        return UserHandle.getUserId(adminInfo.getActivityInfo().applicationInfo.uid);
    }
}
