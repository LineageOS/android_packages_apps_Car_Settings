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

import static com.android.car.settings.enterprise.EnterpriseUtils.getAdminWithinPackage;
import static com.android.car.settings.enterprise.EnterpriseUtils.getDeviceAdminInfo;

import android.app.Activity;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.SettingsFragment;

/**
 * A screen that shows details about a device administrator.
 */
//TODO(b/186054346): add unit test
public final class DeviceAdminDetailsFragment extends SettingsFragment {

    private static final Logger LOG = new Logger(DeviceAdminDetailsFragment.class);

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_admin_details;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = requireActivity();
        Intent intent = activity.getIntent();

        ComponentName admin = (ComponentName)
                intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        if (admin == null) {
            String adminPackage = intent
                    .getStringExtra(DeviceAdminDetailsActivity.EXTRA_DEVICE_ADMIN_PACKAGE_NAME);
            if (adminPackage == null) {
                LOG.w("Finishing " + activity + " as its intent doesn't have "
                        +  DevicePolicyManager.EXTRA_DEVICE_ADMIN + " or "
                        + DeviceAdminDetailsActivity.EXTRA_DEVICE_ADMIN_PACKAGE_NAME);
                activity.finish();
                return;
            }
            admin = getAdminWithinPackage(context, adminPackage);
            if (admin == null) {
                LOG.w("Finishing " + activity + " as there is no active admin for " + adminPackage);
                activity.finish();
                return;
            }
        }

        DeviceAdminInfo deviceAdminInfo = getDeviceAdminInfo(context, admin);
        if (admin == null) {
            LOG.w("Finishing " + activity + " as it could not get DeviceAdminInfo for "
                    + admin.flattenToShortString());
            activity.finish();
            return;
        }

        LOG.d("Admin: " + deviceAdminInfo);

        use(DeviceAdminDetailsHeaderPreferenceController.class,
                R.string.pk_device_admin_details_header).setDeviceAdmin(deviceAdminInfo);
        use(DeviceAdminDetailsWarningPreferenceController.class,
                R.string.pk_device_admin_details_warning).setDeviceAdmin(deviceAdminInfo);
        use(DeviceAdminDetailsSupportPreferenceController.class,
                R.string.pk_device_admin_details_support).setDeviceAdmin(deviceAdminInfo);
    }
}
