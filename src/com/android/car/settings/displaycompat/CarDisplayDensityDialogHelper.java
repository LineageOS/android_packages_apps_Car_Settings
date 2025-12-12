/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.displaycompat;

import static android.car.Car.PERMISSION_MANAGE_DISPLAY_COMPATIBILITY;

import android.Manifest;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.car.content.pm.CarPackageManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;

/**
 * Helper to help setup the dialog for setting the Display Density.
 */
public class CarDisplayDensityDialogHelper extends TwoColumnRadioDialogHelper {
    private static final String TAG = "CarDisplayDensityDH";
    private final Context mContext;
    private final IActivityManager mIActivityManager;
    private final CarPackageManager mCarPackageManager;
    private final Runnable mDismissDialogRunnable;
    private final ComponentName mTargetComponentName;
    private final int mTargetUserId;
    private final int mTargetDisplayId;

    /**
     * @param dismissDialogRunnable {@link Runnable} which will be called when the dialog need to be
     *                              dismissed.
     */
    public CarDisplayDensityDialogHelper(@NonNull Context context,
            @NonNull CarPackageManager carPackageManager, @NonNull Runnable dismissDialogRunnable,
            @NonNull ComponentName componentName, int userId, int displayId) {
        this(context, ActivityManager.getService(), carPackageManager, dismissDialogRunnable,
                componentName, userId, displayId);
    }

    @VisibleForTesting
    public CarDisplayDensityDialogHelper(@NonNull Context context,
            @NonNull IActivityManager iActivityManager,
            @NonNull CarPackageManager carPackageManager, @NonNull Runnable dismissDialogRunnable,
            @NonNull ComponentName componentName, int userId, int displayId) {
        super(dismissDialogRunnable);
        mContext = context;
        mCarPackageManager = carPackageManager;
        mIActivityManager = iActivityManager;
        mDismissDialogRunnable = dismissDialogRunnable;
        mTargetComponentName = componentName;
        mTargetUserId = userId;
        mTargetDisplayId = displayId;
    }

    @Override
    protected void setTitle(@NonNull TextView titleView) {
        titleView.setText(R.string.display_density_dialog_title);
    }

    @Override
    protected void setDescriptionMessage(@NonNull TextView descriptionMessageTextView) {
        descriptionMessageTextView.setText(R.string.display_density_dialog_message);
    }

    @Override
    protected void setupRadioButtons(@NonNull RadioGroup radioGroup) {
        View.inflate(radioGroup.getContext(),
                R.layout.display_density_dialog_radio_buttons, radioGroup);
    }

    @RequiresPermission(allOf = {PERMISSION_MANAGE_DISPLAY_COMPATIBILITY,
            Manifest.permission.QUERY_ALL_PACKAGES})
    @Override
    protected void setDefaultSelection(@NonNull RadioGroup radioGroup) {
        updateRadioGroupBasedOnCurrentDisplayDensity(radioGroup);
    }

    @RequiresPermission(allOf = {PERMISSION_MANAGE_DISPLAY_COMPATIBILITY,
            Manifest.permission.QUERY_ALL_PACKAGES, Manifest.permission.INTERACT_ACROSS_USERS})
    @Override
    protected void submitResult(@NonNull RadioGroup radioGroup, @Nullable String selectedTagValue) {
        if (selectedTagValue == null) {
            Log.d(TAG, "No display density value selected");
            return;
        }
        float selectedDisplayDensityValue;
        try {
            selectedDisplayDensityValue = Float.parseFloat(selectedTagValue);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Incorrect Display Density value supplied: " + selectedTagValue, e);
        }

        try {
            Log.d(TAG, "Setting the display density for cmp: " + mTargetComponentName
                    + ",  userId: " + mTargetUserId + " with the value: "
                    + selectedDisplayDensityValue);
            mCarPackageManager.setDensityScaleFactor(mTargetComponentName.getPackageName(),
                    mTargetUserId, mTargetDisplayId, selectedDisplayDensityValue);

            mDismissDialogRunnable.run();

            // app should be restarted after setting the display density
            mIActivityManager.stopAppForUser(mTargetComponentName.getPackageName(), mTargetUserId);

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(mTargetComponentName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.of(mTargetUserId));
        } catch (RemoteException e) {
            Log.e(TAG, "Exception when showing the dialog for cmp: " + mTargetComponentName, e);
            Toast.makeText(mContext, R.string.display_density_set_error, Toast.LENGTH_LONG).show();
        } finally {
            // make sure the dialog is always closed
            mDismissDialogRunnable.run();
        }
    }

    @RequiresPermission(allOf = {PERMISSION_MANAGE_DISPLAY_COMPATIBILITY,
            Manifest.permission.QUERY_ALL_PACKAGES})
    private void updateRadioGroupBasedOnCurrentDisplayDensity(@NonNull RadioGroup radioGroup) {
        float currentDisplayDensityFactor = mCarPackageManager.getDensityScaleFactor(
                mTargetComponentName.getPackageName(), mTargetUserId, mTargetDisplayId);
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            View child = radioGroup.getChildAt(i);
            if (child == null) {
                continue;
            }
            float tag = Float.parseFloat(child.getTag().toString());
            if (tag == currentDisplayDensityFactor) {
                radioGroup.check(child.getId());
                return;
            }
        }
    }
}
