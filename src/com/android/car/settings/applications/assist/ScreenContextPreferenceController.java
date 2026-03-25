/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.car.settings.applications.assist;

import android.app.AppOpsManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;

import androidx.preference.TwoStatePreference;

import com.android.car.settings.common.FragmentController;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.AssistUtils;

import java.util.Collections;
import java.util.List;

/** Toggles the assistant's ability to use the screen content and app data for context. */
public class ScreenContextPreferenceController extends AssistConfigBasePreferenceController {

    private final AppOpsManager mAppOpsManager;
    private final AssistUtils mAssistUtils;

    public ScreenContextPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        this(context, preferenceKey, fragmentController, uxRestrictions, new AssistUtils(context));
    }

    @VisibleForTesting
    ScreenContextPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions,
            AssistUtils assistUtils) {
        super(context, preferenceKey, fragmentController, uxRestrictions, assistUtils);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mAssistUtils = assistUtils;
    }

    private final AppOpsManager.OnOpChangedListener mOpChangedListener =
            (op, packageName) -> new Handler(Looper.getMainLooper()).post(this::refreshUi);

    @Override
    protected void onStartInternal() {
        super.onStartInternal();
        mAppOpsManager.startWatchingMode(AppOpsManager.OPSTR_ASSIST_STRUCTURE, null,
                mOpChangedListener);
    }

    @Override
    protected void onStopInternal() {
        mAppOpsManager.stopWatchingMode(mOpChangedListener);
        super.onStopInternal();
    }

    @Override
    protected void updateState(TwoStatePreference preference) {
        preference.setChecked(isAssistContextEnabled());
    }

    @Override
    protected boolean handlePreferenceChanged(TwoStatePreference preference, Object newValue) {
        setAssistContextEnabled((boolean) newValue);
        return true;
    }

    @Override
    protected List<Uri> getSettingUris() {
        return Collections.emptyList();
    }

    private boolean isAssistContextEnabled() {
        ComponentName currentAssistant = mAssistUtils.getAssistComponentForUser(
                UserHandle.myUserId());
        if (currentAssistant == null) {
            return false;
        }

        int uid;
        try {
            uid = getContext().getPackageManager().getPackageUidAsUser(
                    currentAssistant.getPackageName(), UserHandle.myUserId());
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        int mode = mAppOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_ASSIST_STRUCTURE, uid,
                currentAssistant.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_DEFAULT;
    }

    private void setAssistContextEnabled(boolean enabled) {
        ComponentName currentAssistant = mAssistUtils.getAssistComponentForUser(
                UserHandle.myUserId());
        if (currentAssistant == null) {
            return;
        }

        int uid;
        try {
            uid = getContext().getPackageManager().getPackageUidAsUser(
                    currentAssistant.getPackageName(), UserHandle.myUserId());
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }

        int newMode = enabled ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED;
        mAppOpsManager.setUidMode(AppOpsManager.OPSTR_ASSIST_STRUCTURE, uid, newMode);
    }
}
