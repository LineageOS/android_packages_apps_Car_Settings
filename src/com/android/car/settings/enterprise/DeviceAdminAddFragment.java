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
public final class DeviceAdminAddFragment extends SettingsFragment {

    private static final Logger LOG = new Logger(DeviceAdminAddFragment.class);

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_admin_add;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = requireActivity();
        Intent intent = activity.getIntent();

        ComponentName admin = (ComponentName)
                intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        LOG.d("Admin using " + DevicePolicyManager.EXTRA_DEVICE_ADMIN + ": " + admin);
        if (admin == null) {
            String adminPackage = intent
                    .getStringExtra(DeviceAdminAddActivity.EXTRA_DEVICE_ADMIN_PACKAGE_NAME);
            LOG.d("Admin package using " + DeviceAdminAddActivity.EXTRA_DEVICE_ADMIN_PACKAGE_NAME
                    + ": " + adminPackage);
            if (adminPackage == null) {
                LOG.w("Finishing " + activity + " as its intent doesn't have "
                        +  DevicePolicyManager.EXTRA_DEVICE_ADMIN + " or "
                        + DeviceAdminAddActivity.EXTRA_DEVICE_ADMIN_PACKAGE_NAME);
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
        LOG.d("Admin: " + admin + " DeviceAdminInfo: " + deviceAdminInfo);

        if (deviceAdminInfo == null) {
            LOG.w("Finishing " + activity + " as it could not get DeviceAdminInfo for "
                    + admin.flattenToShortString());
            activity.finish();
            return;
        }

        use(DeviceAdminAddHeaderPreferenceController.class,
                R.string.pk_device_admin_add_header).setDeviceAdmin(deviceAdminInfo);
        ((DeviceAdminAddExplanationPreferenceController) use(
                DeviceAdminAddExplanationPreferenceController.class,
                R.string.pk_device_admin_add_explanation).setDeviceAdmin(deviceAdminInfo))
                        .setExplanation(intent
                                .getCharSequenceExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION));
        use(DeviceAdminAddWarningPreferenceController.class,
                R.string.pk_device_admin_add_warning).setDeviceAdmin(deviceAdminInfo);
        use(DeviceAdminAddPoliciesPreferenceController.class,
                R.string.pk_device_admin_add_policies).setDeviceAdmin(deviceAdminInfo);
        use(DeviceAdminAddSupportPreferenceController.class,
                R.string.pk_device_admin_add_support).setDeviceAdmin(deviceAdminInfo);
        use(DeviceAdminAddCancelPreferenceController.class,
                R.string.pk_device_admin_add_cancel).setDeviceAdmin(deviceAdminInfo);
    }

    // TODO(b/188585303): must override onCreatePreferences() to change title of preference screen
    // to add_device_admin_msg when launched with DEVICE_ADMIN_ADD
}
