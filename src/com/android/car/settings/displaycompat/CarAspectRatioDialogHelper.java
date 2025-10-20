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

import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_16_9;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_3_2;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_4_3;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_APP_DEFAULT;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_DISPLAY_SIZE;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;

import static com.android.systemui.car.Flags.displayCompatibilityV2;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;

/**
 * Helper to help setup the dialog for setting the Aspect Ratio.
 */
public class CarAspectRatioDialogHelper {
    private static final String TAG = "CarAspectRatioADH";
    private final Context mContext;
    private final IPackageManager mIPackageManager;
    private final IActivityManager mIActivityManager;

    public CarAspectRatioDialogHelper(Context context) {
        this(context, AppGlobals.getPackageManager(), ActivityManager.getService());
    }

    @VisibleForTesting
    CarAspectRatioDialogHelper(Context context, IPackageManager packageManager,
            IActivityManager activityManager) {
        mContext = context;
        mIPackageManager = packageManager;
        mIActivityManager = activityManager;
    }

    /**
     * Setup the {@code dialogView} to let the user choose and set the aspect ratio for the given
     * {@code componentName}.
     *
     * @param dismissDialog {@link Runnable} which will be called when the dialog need to be
     *                      dismissed.
     */
    @RequiresPermission(android.Manifest.permission.INTERACT_ACROSS_USERS)
    public void setupAspectRatioDialog(
            @NonNull View dialogView, @NonNull ComponentName componentName, int userId,
            Runnable dismissDialog) {
        if (!displayCompatibilityV2()) {
            return;
        }
        if (mIPackageManager == null) {
            Log.e(TAG, "CarPackageManager is null, the dialog cannot be created");
            return;
        }

        Button buttonClose = dialogView.findViewById(R.id.button_close);
        Button buttonApply = dialogView.findViewById(R.id.button_apply);
        RadioGroup radioGroupAspectRatio = dialogView.findViewById(R.id.aspect_ratio_radio_group);

        if (radioGroupAspectRatio == null || buttonApply == null) {
            throw new IllegalStateException(
                    "Aspect Ratio Dialog requires RadioGroup and accept Button to be present.");
        }

        // only make the apply button available once a selection has been made
        buttonApply.setEnabled(radioGroupAspectRatio.getCheckedRadioButtonId() != -1);
        radioGroupAspectRatio.setOnCheckedChangeListener(
                (radioGroup, checkedId) -> buttonApply.setEnabled(checkedId != -1));
        updateRadioGroupBasedOnCurrentAspectRatio(componentName, userId, radioGroupAspectRatio);

        if (buttonClose != null) {
            buttonClose.setOnClickListener(view -> dismissDialog.run());
        }
        buttonApply.setOnClickListener(view -> {
            int selectedRadioButtonId = radioGroupAspectRatio.getCheckedRadioButtonId();
            int selectedAspectRatioValue = Integer.MIN_VALUE;
            RadioButton selectedRadioButton = dialogView.findViewById(selectedRadioButtonId);
            if (selectedRadioButton != null && selectedRadioButton.getTag() != null) {
                selectedAspectRatioValue = Integer.parseInt(
                        selectedRadioButton.getTag().toString());
            }
            if (selectedAspectRatioValue == Integer.MIN_VALUE) {
                Log.d(TAG, "No aspect ratio value selected");
                return;
            }
            if (!isValidAspectRatio(selectedAspectRatioValue)) {
                throw new IllegalStateException("Incorrect Aspect Ratio value supplied.");
            }

            try {
                Log.d(TAG, "Setting the UserMinAspectRatio for cmp: " + componentName
                        + ",  userId: " + userId + " with the value: "
                        + selectedAspectRatioValue);
                mIPackageManager.setUserMinAspectRatio(componentName.getPackageName(), userId,
                        selectedAspectRatioValue);

                dismissDialog.run();

                // app should be restarted after setting the aspect ratio
                mIActivityManager.stopAppForUser(componentName.getPackageName(), userId);

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(componentName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivityAsUser(intent, UserHandle.of(userId));

            } catch (RemoteException e) {
                Log.e(TAG, "Exception when showing the dialog for cmp: " + componentName, e);
            } finally {
                // make sure the dialog is always closed
                dismissDialog.run();
            }
        });
    }

    private boolean isValidAspectRatio(int aspectRatio) {
        return aspectRatio == USER_MIN_ASPECT_RATIO_UNSET
                || aspectRatio == USER_MIN_ASPECT_RATIO_SPLIT_SCREEN
                || aspectRatio == USER_MIN_ASPECT_RATIO_DISPLAY_SIZE
                || aspectRatio == USER_MIN_ASPECT_RATIO_4_3
                || aspectRatio == USER_MIN_ASPECT_RATIO_16_9
                || aspectRatio == USER_MIN_ASPECT_RATIO_3_2
                || aspectRatio == USER_MIN_ASPECT_RATIO_FULLSCREEN
                || aspectRatio == USER_MIN_ASPECT_RATIO_APP_DEFAULT;
    }

    private void updateRadioGroupBasedOnCurrentAspectRatio(
            @NonNull ComponentName componentName, int userId, @NonNull RadioGroup radioGroup) {
        try {
            int currentAspectRatioSelection = mIPackageManager.getUserMinAspectRatio(
                    componentName.getPackageName(), userId);
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                View child = radioGroup.getChildAt(i);
                if (child == null) {
                    continue;
                }
                int tag = Integer.parseInt(child.getTag().toString());
                if (!isValidAspectRatio(tag)) {
                    continue;
                }
                if (tag == currentAspectRatioSelection) {
                    radioGroup.check(child.getId());
                    return;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot get min aspect ratio for pkg: " + componentName.getPackageName(), e);
        }
    }
}
