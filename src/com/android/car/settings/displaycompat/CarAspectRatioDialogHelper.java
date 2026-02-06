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
import com.android.car.settings.applications.appinfo.AspectRatioManager;

/**
 * Helper to help setup the dialog for setting the Aspect Ratio.
 */
public class CarAspectRatioDialogHelper extends TwoColumnRadioDialogHelper {
    private static final String TAG = "CarAspectRatioDH";
    private final Context mContext;
    private final Runnable mDismissDialogRunnable;
    private final ComponentName mTargetComponentName;
    private final int mTargetUserId;
    private final AspectRatioManager mAspectRatioManager;

    /**
     * @param dismissDialogRunnable {@link Runnable} which will be called when the dialog need to be
     *                              dismissed.
     */
    public CarAspectRatioDialogHelper(@NonNull Context context,
            @NonNull Runnable dismissDialogRunnable, @NonNull ComponentName componentName,
            int userId) {
        this(context, dismissDialogRunnable, componentName, userId,
            new AspectRatioManager(context));
    }

    @VisibleForTesting
    public CarAspectRatioDialogHelper(@NonNull Context context,
            @NonNull Runnable dismissDialogRunnable, @NonNull ComponentName componentName,
            int userId, @NonNull AspectRatioManager aspectRatioManager) {
        super(dismissDialogRunnable);
        mContext = context;
        mDismissDialogRunnable = dismissDialogRunnable;
        mTargetComponentName = componentName;
        mTargetUserId = userId;
        mAspectRatioManager = aspectRatioManager;
    }


    @Override
    protected void setTitle(@NonNull TextView titleView) {
        titleView.setText(R.string.aspect_ratio_dialog_title);
    }

    @Override
    protected void setDescriptionMessage(@NonNull TextView descriptionMessageTextView) {
        descriptionMessageTextView.setText(R.string.aspect_ratio_dialog_message);
    }

    @Override
    protected void setupRadioButtons(@NonNull RadioGroup radioGroup) {
        View.inflate(radioGroup.getContext(),
                R.layout.aspect_ratio_dialog_radio_buttons, radioGroup);
    }

    @Override
    protected void setDefaultSelection(@NonNull RadioGroup radioGroup) {
        updateRadioGroupBasedOnCurrentAspectRatio(radioGroup);
    }

    @RequiresPermission(android.Manifest.permission.INTERACT_ACROSS_USERS)
    @Override
    protected void submitResult(@NonNull RadioGroup radioGroup, @Nullable String selectedTagValue) {
        if (selectedTagValue == null) {
            Log.d(TAG, "No aspect ratio value selected");
            return;
        }
        int selectedAspectRatioValue;
        try {
            selectedAspectRatioValue = Integer.parseInt(selectedTagValue);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Incorrect Aspect Ratio value supplied: " + selectedTagValue, e);
        }
        if (!isValidAspectRatio(selectedAspectRatioValue)) {
            throw new IllegalStateException(
                    "Incorrect Aspect Ratio value supplied: " + selectedTagValue);
        }

        try {
            Log.d(TAG, "Setting the UserMinAspectRatio for cmp: " + mTargetComponentName
                    + ",  userId: " + mTargetUserId + " with the value: "
                    + selectedAspectRatioValue);
            mAspectRatioManager.setUserMinAspectRatio(
                    mTargetComponentName.getPackageName(),
                    mTargetUserId,
                    selectedAspectRatioValue);
            mDismissDialogRunnable.run();
            mAspectRatioManager.stopApp(mTargetComponentName.getPackageName());

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(mTargetComponentName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.of(mTargetUserId));

        } catch (RemoteException e) {
            Log.e(TAG, "Exception when showing the dialog for cmp: " + mTargetComponentName, e);
            Toast.makeText(mContext, R.string.aspect_ratio_set_error, Toast.LENGTH_LONG).show();
        } finally {
            // make sure the dialog is always closed
            mDismissDialogRunnable.run();
        }
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
            @NonNull RadioGroup radioGroup) {
        try {
            int currentAspectRatioSelection = mAspectRatioManager
                    .getUserMinAspectRatioValue(mTargetComponentName.getPackageName(),
                            mTargetUserId);
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
            Log.e(TAG,
                    "Cannot get min aspect ratio for pkg: " + mTargetComponentName.getPackageName(),
                    e);
        }
    }
}
