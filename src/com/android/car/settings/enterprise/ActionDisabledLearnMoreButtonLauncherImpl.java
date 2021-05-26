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

import static java.util.Objects.requireNonNull;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.car.ui.AlertDialogBuilder;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.enterprise.ActionDisabledLearnMoreButtonLauncher;

/**
 * Car's implementation of {@link ActionDisabledLearnMoreButtonLauncher}.
 */
final class ActionDisabledLearnMoreButtonLauncherImpl
        implements ActionDisabledLearnMoreButtonLauncher {

    private static final Logger LOG = new Logger(ActionDisabledLearnMoreButtonLauncherImpl.class);

    // TODO(b/188836559): move logic below to superclass so this method is just
    // setLearnMore(context, enforcedAdmin). Or even better: setLearnMore(builder, runnable)
    @Override
    public void setupLearnMoreButtonToShowAdminPolicies(Context context, Object alertDialogBuilder,
            int enforcementAdminUserId, EnforcedAdmin enforcedAdmin) {
        requireNonNull(context, "context cannot be null");
        requireNonNull(alertDialogBuilder, "alertDialogBuilder cannot be null");
        requireNonNull(enforcedAdmin, "enforcedAdmin cannot be null");

        // The "Learn more" button appears only if the restriction is enforced by an admin in the
        // same profile group. Otherwise the admin package and its policies are not accessible to
        // the current user.
        UserManager um = UserManager.get(context);
        if (um.isSameProfileGroup(enforcementAdminUserId, um.getUserHandle())) {
            ((AlertDialogBuilder) alertDialogBuilder).setNeutralButton(R.string.learn_more,
                    (d, i) -> showAdminPolicies(context, enforcedAdmin));
        }
    }

    @Override
    public void setupLearnMoreButtonToLaunchHelpPage(Context context, Object alertDialogBuilder,
            String url) {
        requireNonNull(context, "context cannot be null");
        requireNonNull(alertDialogBuilder, "alertDialogBuilder cannot be null");
        requireNonNull(url, "url cannot be null");

        context.startActivityAsUser(createLearnMoreIntent(url), UserHandle.of(context.getUserId()));
    }

    // TODO(b/188836559): move logic to superclass ?
    private static Intent createLearnMoreIntent(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    }

    // TODO(b/188836559): move logic to superclass
    private void showAdminPolicies(Context context, EnforcedAdmin enforcedAdmin) {
        if (enforcedAdmin.component != null) {
            LOG.w("DeviceAdminInfoActivity not supported yet");
            Intent intent = new Intent();
            intent.setClass(context, DeviceAdminDetailsActivity.class);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    enforcedAdmin.component);
            intent.putExtra(DeviceAdminDetailsActivity.EXTRA_CALLED_FROM_SUPPORT_DIALOG, true);
            // DeviceAdminInfoActivity class may need to run as managed profile.
            context.startActivityAsUser(intent, enforcedAdmin.user);
        } else {
            // TODO(b/185183049): launch DeviceAdminSettingsActivity
            LOG.w("DeviceAdminSettingsActivity not supported yet");
        }
    }
}
