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
package com.android.car.settings.accounts;

import android.accounts.Account;
import android.annotation.DrawableRes;
import android.annotation.NonNull;
import android.annotation.StringRes;
import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.view.View;
import android.widget.ImageView;

import com.android.car.list.IconTextLineItem;
import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

/**
 * Shows an + Add account option in Users & Accounts page.
 */
public class AddAccountLineItem extends IconTextLineItem {
    private final Context mContext;
    @DrawableRes
    private final int mIconRes;
    private final String mTitle;
    private final BaseFragment.FragmentController mFragmentController;

    public AddAccountLineItem(
            String title,
            @DrawableRes int iconRes,
            @NonNull Context context,
            BaseFragment.FragmentController fragmentController) {
        super(title);
        mTitle = title;
        mContext = context;
        mIconRes = iconRes;
        mFragmentController = fragmentController;
    }

    @Override
    public void bindViewHolder(IconTextLineItem.ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.titleView.setText(mTitle);
    }

    @Override
    public void onClick(View view) {
        mFragmentController.launchFragment(ChooseAccountFragment.newInstance());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isExpandable() {
        return true;
    }

    @Override
    public CharSequence getDesc() {
        return null;
    }

    @Override
    public void setIcon(ImageView iconView) {
        iconView.setImageResource(mIconRes);
    }
}
