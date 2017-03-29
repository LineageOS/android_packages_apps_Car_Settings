/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.car.settings.applications;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.ImageView;

import com.android.car.settings.common.AnimationUtil;
import com.android.car.settings.common.IconTextLineItem;

/**
 * Represents an application in application settings page.
 */
public class ApplicationLineItem extends IconTextLineItem {
    private final ResolveInfo mResolveInfo;
    private final Context mContext;
    private final PackageManager mPm;

    public ApplicationLineItem(
            @NonNull Context context, PackageManager pm, ResolveInfo resolveInfo) {
        super(resolveInfo.loadLabel(pm));
        mContext = context;
        mPm = pm;
        mResolveInfo = resolveInfo;
    }


    @Override
    public void onClick() {
        Intent intent = new Intent(mContext, ApplicationDetailActivity.class);
        intent.putExtra(
                ApplicationDetailActivity.APPLICATION_INFO_KEY, mResolveInfo);
        mContext.startActivity(
                intent, AnimationUtil.slideInFromRightOption(mContext).toBundle());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public CharSequence getDesc() {
        return null;
    }

    @Override
    public void setIcon(ImageView iconView) {
        iconView.setImageDrawable(mResolveInfo.loadIcon(mPm));
    }
}
