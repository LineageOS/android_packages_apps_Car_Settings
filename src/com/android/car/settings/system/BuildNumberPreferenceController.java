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

package com.android.car.settings.system;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.NoSetupPreferenceController;
import com.android.car.settings.development.DevelopmentSettingsUtil;

/** Updates the build number entry summary with the build number. */
public class BuildNumberPreferenceController extends NoSetupPreferenceController implements
        LifecycleObserver {

    private final CarUserManagerHelper mCarUserManagerHelper;
    private Toast mDevHitToast;
    private int mDevHitCountdown;

    public BuildNumberPreferenceController(Context context,
            String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
        mCarUserManagerHelper = new CarUserManagerHelper(mContext);
    }

    /**
     * Reset the toast and counter which tracks how many more clicks until development settings is
     * enabled.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mDevHitToast = null;
        mDevHitCountdown = DevelopmentSettingsUtil.isDevelopmentSettingsEnabled(mContext,
                mCarUserManagerHelper) ? -1 : getTapsToBecomeDeveloper();
    }

    @Override
    public CharSequence getSummary() {
        return Build.DISPLAY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        if (!mCarUserManagerHelper.isCurrentProcessAdminUser()
                && !mCarUserManagerHelper.isCurrentProcessDemoUser()) {
            return false;
        }

        if (!DevelopmentSettingsUtil.isDeviceProvisioned(mContext)) {
            return false;
        }

        if (mDevHitCountdown > 0) {
            mDevHitCountdown--;
            if (mDevHitCountdown == 0) {
                DevelopmentSettingsUtil.setDevelopmentSettingsEnabled(mContext, true);
                showToast(mContext.getString(R.string.show_dev_on), Toast.LENGTH_LONG);
            } else if (mDevHitCountdown <= getTapsToBecomeDeveloper() - getTapsToShowToast()) {
                showToast(mContext.getResources().getQuantityString(R.plurals.show_dev_countdown,
                        mDevHitCountdown, mDevHitCountdown), Toast.LENGTH_SHORT);
            }
        } else if (mDevHitCountdown < 0) {
            showToast(mContext.getString(R.string.show_dev_already), Toast.LENGTH_LONG);
        }
        return true;
    }

    private void showToast(String text, @Toast.Duration int duration) {
        if (mDevHitToast != null) {
            mDevHitToast.cancel();
        }
        mDevHitToast = Toast.makeText(mContext, text, duration);
        mDevHitToast.show();
    }

    private int getTapsToBecomeDeveloper() {
        return mContext.getResources().getInteger(R.integer.enable_developer_settings_click_count);
    }

    private int getTapsToShowToast() {
        return mContext.getResources().getInteger(
                R.integer.enable_developer_settings_clicks_to_show_toast_count);
    }
}
