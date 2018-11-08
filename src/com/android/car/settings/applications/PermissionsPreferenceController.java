/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.applications;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.icu.text.ListFormatter;
import android.text.TextUtils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.NoSetupPreferenceController;
import com.android.settingslib.applications.PermissionsSummaryHelper;

import java.util.ArrayList;
import java.util.List;

/** Business logic for the permissions entry in the application details settings. */
public class PermissionsPreferenceController extends NoSetupPreferenceController implements
        LifecycleObserver {

    private static final Logger LOG = new Logger(PermissionsPreferenceController.class);

    private ResolveInfo mResolveInfo;
    private String mSummary;
    private Preference mPreference;

    public PermissionsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    /** Check that resolveInfo is set on create. */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        if (mResolveInfo == null) {
            throw new IllegalStateException(
                    "ResolveInfo should be set before calling this function");
        }
    }

    /**
     * Set the resolve info, which is used to find the package name to open the permissions
     * selection screen.
     */
    public void setResolveInfo(ResolveInfo resolveInfo) {
        mResolveInfo = resolveInfo;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());

        // This call needs to be here, not onCreate, so that the summary is updated every time
        // the preference is displayed.
        PermissionsSummaryHelper.getPermissionSummary(mContext,
                mResolveInfo.activityInfo.packageName, mPermissionCallback);
    }

    @Override
    public CharSequence getSummary() {
        if (TextUtils.isEmpty(mSummary)) {
            return mContext.getString(R.string.computing_size);
        }
        return mSummary;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (mPreference != preference) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mResolveInfo.activityInfo.packageName);
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LOG.w("No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
        return true;
    }

    private final PermissionsSummaryHelper.PermissionsResultCallback mPermissionCallback =
            new PermissionsSummaryHelper.PermissionsResultCallback() {
                @Override
                public void onPermissionSummaryResult(int standardGrantedPermissionCount,
                        int requestedPermissionCount, int additionalGrantedPermissionCount,
                        List<CharSequence> grantedGroupLabels) {
                    Resources res = mContext.getResources();

                    if (requestedPermissionCount == 0) {
                        mSummary = res.getString(
                                R.string.runtime_permissions_summary_no_permissions_requested);
                    } else {
                        ArrayList<CharSequence> list = new ArrayList<>(grantedGroupLabels);
                        if (additionalGrantedPermissionCount > 0) {
                            // N additional permissions.
                            list.add(res.getQuantityString(
                                    R.plurals.runtime_permissions_additional_count,
                                    additionalGrantedPermissionCount,
                                    additionalGrantedPermissionCount));
                        }
                        if (list.isEmpty()) {
                            mSummary = res.getString(
                                    R.string.runtime_permissions_summary_no_permissions_granted);
                        } else {
                            mSummary = ListFormatter.getInstance().format(list);
                        }
                    }
                    updateState(mPreference);
                }
            };
}
