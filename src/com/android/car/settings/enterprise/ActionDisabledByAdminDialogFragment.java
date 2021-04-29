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

import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_FINANCED;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.IconDrawableFactory;
import android.util.Log;

import com.android.car.settings.R;
import com.android.car.ui.AlertDialogBuilder;
import com.android.car.ui.preference.CarUiDialogFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

/**
 * Shows a dialog explain that an action is not enabled due to restrictions imposed by an active
 * device administrator.
 */
public final class ActionDisabledByAdminDialogFragment extends CarUiDialogFragment {

    private static final String TAG = ActionDisabledByAdminDialogFragment.class.getSimpleName();

    private static final String EXTRA_RESTRICTION = TAG + "_restriction";
    private static final String EXTRA_USER_ID = TAG + "_userId";

    private String mRestriction;

    @UserIdInt
    private int mUserId;

    /**
     * Gets the dialog for the given user and restriction.
     */
    public static ActionDisabledByAdminDialogFragment newInstance(String restriction,
            @UserIdInt int userId) {
        ActionDisabledByAdminDialogFragment instance = new ActionDisabledByAdminDialogFragment();
        instance.mRestriction = restriction;
        instance.mUserId = userId;
        return instance;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mRestriction = savedInstanceState.getString(EXTRA_RESTRICTION);
            mUserId = savedInstanceState.getInt(EXTRA_USER_ID);
        }
        return initialize(getContext()).create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_RESTRICTION, mRestriction);
        outState.putInt(EXTRA_USER_ID, mUserId);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    }

    private AlertDialogBuilder initialize(Context context) {
        EnforcedAdmin enforcedAdmin = RestrictedLockUtilsInternal
                .checkIfRestrictionEnforced(context, mRestriction, mUserId);
        AlertDialogBuilder builder = new AlertDialogBuilder(context)
                .setPositiveButton(R.string.okay, /* listener= */ null);
        maybeSetLearnMoreButton(context, builder, enforcedAdmin);
        initializeDialogViews(context, builder, enforcedAdmin.component,
                getEnforcementAdminUserId(enforcedAdmin));
        return builder;
    }

    // NOTE: methods below were copied from phone Settings
    // (com.android.settings.enterprise.ActionDisabledByAdminDialogHelper), but adjusted to
    // use a AlertDialogBuilder directly, instead of an Activity hosting a dialog.

    private static @UserIdInt int getEnforcementAdminUserId(@NonNull EnforcedAdmin admin) {
        return admin.user == null ? UserHandle.USER_NULL : admin.user.getIdentifier();
    }

    private void maybeSetLearnMoreButton(Context context, AlertDialogBuilder builder,
            EnforcedAdmin enforcedAdmin) {
        // The "Learn more" button appears only if the restriction is enforced by an admin in the
        // same profile group. Otherwise the admin package and its policies are not accessible to
        // the current user.
        UserManager um = UserManager.get(context.getApplicationContext());
        if (um.isSameProfileGroup(getEnforcementAdminUserId(enforcedAdmin), um.getUserHandle())) {
            builder.setNeutralButton(R.string.learn_more, (d, i) ->
                    showAdminPolicies(context, enforcedAdmin));
        }
    }

    private void initializeDialogViews(Context context, AlertDialogBuilder builder,
            ComponentName admin, @UserIdInt int userId) {
        setAdminSupportIcon(context, builder, admin, userId);

        if (isNotCurrentUserOrProfile(context, admin, userId)) {
            admin = null;
        }

        setAdminSupportTitle(context, builder, mRestriction);

        UserHandle user = userId == UserHandle.USER_NULL ? null : UserHandle.of(userId);

        setAdminSupportDetails(context, builder, new EnforcedAdmin(admin, user));
    }

    private boolean isNotCurrentUserOrProfile(Context context, ComponentName admin,
            @UserIdInt int userId) {
        return !RestrictedLockUtilsInternal.isAdminInCurrentUserOrProfile(context, admin)
                || !RestrictedLockUtils.isCurrentUserOrProfile(context, userId);
    }

    private void setAdminSupportIcon(Context context, AlertDialogBuilder builder,
            ComponentName admin, @UserIdInt int userId) {
        if (isNotCurrentUserOrProfile(context, admin, userId)) {
            builder.setIcon(context.getDrawable(com.android.internal.R.drawable.ic_info));
        } else {
            Drawable badgedIcon = getBadgedIcon(
                    IconDrawableFactory.newInstance(context),
                    context.getPackageManager(),
                    admin.getPackageName(),
                    userId);
            builder.setIcon(badgedIcon);
        }
    }

    private void setAdminSupportTitle(Context context, AlertDialogBuilder builder,
            String restriction) {
        if (isFinancedDevice(context)) {
            builder.setTitle(R.string.disabled_by_policy_title_financed_device);
            return;
        }
        if (restriction == null) {
            builder.setTitle(R.string.disabled_by_policy_title);
            return;
        }
        switch (restriction) {
            case UserManager.DISALLOW_ADJUST_VOLUME:
                builder.setTitle(R.string.disabled_by_policy_title_adjust_volume);
                break;
            case UserManager.DISALLOW_OUTGOING_CALLS:
                builder.setTitle(R.string.disabled_by_policy_title_outgoing_calls);
                break;
            case UserManager.DISALLOW_SMS:
                builder.setTitle(R.string.disabled_by_policy_title_sms);
                break;
            case DevicePolicyManager.POLICY_DISABLE_CAMERA:
                builder.setTitle(R.string.disabled_by_policy_title_camera);
                break;
            case DevicePolicyManager.POLICY_DISABLE_SCREEN_CAPTURE:
                builder.setTitle(R.string.disabled_by_policy_title_screen_capture);
                break;
            case DevicePolicyManager.POLICY_SUSPEND_PACKAGES:
                builder.setTitle(R.string.disabled_by_policy_title_suspend_packages);
                break;
            default:
                // Use general text if no specialized title applies
                builder.setTitle(R.string.disabled_by_policy_title);
        }
    }

    private void setAdminSupportDetails(Context context, AlertDialogBuilder builder,
            @Nullable EnforcedAdmin enforcedAdmin) {
        if (enforcedAdmin == null || enforcedAdmin.component == null) {
            Log.i(TAG, "setAdminSupportDetails(): no admin on " + enforcedAdmin);
            return;
        }
        CharSequence supportMessage = null;
        if (!RestrictedLockUtilsInternal.isAdminInCurrentUserOrProfile(context,
                enforcedAdmin.component) || !RestrictedLockUtils.isCurrentUserOrProfile(
                        context, getEnforcementAdminUserId(enforcedAdmin))) {
            enforcedAdmin.component = null;
        } else {
            if (enforcedAdmin.user == null) {
                enforcedAdmin.user = UserHandle.of(UserHandle.myUserId());
            }
            if (UserHandle.isSameApp(Process.myUid(), Process.SYSTEM_UID)) {
                supportMessage = context.getSystemService(DevicePolicyManager.class)
                        .getShortSupportMessageForUser(enforcedAdmin.component,
                                getEnforcementAdminUserId(enforcedAdmin));
            }
        }
        if (supportMessage != null) {
            builder.setMessage(supportMessage);
        }
    }

    private void showAdminPolicies(Context context, EnforcedAdmin enforcedAdmin) {
        if (enforcedAdmin.component != null) {
            // TODO(b/186054346): launch DeviceAdminInfoActivity
            Log.w(TAG, "DeviceAdminInfoActivity not supported yet");
//            Intent intent = new Intent();
//            intent.setClass(context, DeviceAdminInfoActivity.class);
//            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
//                    enforcedAdmin.component);
//            intent.putExtra(DeviceAdminInfoActivity.EXTRA_CALLED_FROM_SUPPORT_DIALOG, true);
//            // DeviceAdminInfoActivity class may need to run as managed profile.
//            context.startActivityAsUser(intent, enforcedAdmin.user);
        } else {
            // TODO(b/185183049): launch DeviceAdminSettingsActivity
            Log.w(TAG, "DeviceAdminSettingsActivity not supported yet");
        }
    }

    private boolean isFinancedDevice(Context context) {
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        return dpm.isDeviceManaged() && dpm.getDeviceOwnerType(
                dpm.getDeviceOwnerComponentOnAnyUser()) == DEVICE_OWNER_TYPE_FINANCED;
    }

    // Copied from com.android.settings.Utils
    private static Drawable getBadgedIcon(IconDrawableFactory iconDrawableFactory,
            PackageManager packageManager, String packageName, int userId) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfoAsUser(
                    packageName, PackageManager.GET_META_DATA, userId);
            return iconDrawableFactory.getBadgedIcon(appInfo, userId);
        } catch (PackageManager.NameNotFoundException e) {
            return packageManager.getDefaultActivityIcon();
        }
    }
}
