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

import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

/**
 * Represents an application in application settings page.
 */
public class ApplicationListItem extends TextListItem {

    public ApplicationListItem(
            @NonNull Context context,
            PackageManager pm,
            ResolveInfo resolveInfo,
            BaseFragment.FragmentController fragmentController) {
        this(context, pm, resolveInfo, fragmentController, true);
    }

    public ApplicationListItem(
            @NonNull Context context,
            PackageManager pm,
            ResolveInfo resolveInfo,
            BaseFragment.FragmentController fragmentController,
            boolean clickable) {
        super(context);
        setTitle(resolveInfo.loadLabel(pm).toString());
        setPrimaryActionIcon(resolveInfo.loadIcon(pm), TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        if (clickable) {
            setSupplementalIcon(R.drawable.ic_chevron_right, /* showDivider= */ false);
            setOnClickListener(v ->
                    fragmentController.launchFragment(
                            ApplicationDetailFragment.getInstance(resolveInfo)));
        }
    }
}
