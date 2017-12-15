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
import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.android.car.list.IconTextLineItem;
import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

/**
 * Builds the account list in Settings page.
 */
public class AccountLineItem extends IconTextLineItem {
    private final Context mContext;
    private final UserInfo mUserInfo;
    private final Account mAccount;
    private final BaseFragment.FragmentController mFragmentController;

    public AccountLineItem(
            @NonNull Context context,
            UserInfo userInfo,
            Account account,
            BaseFragment.FragmentController fragmentController) {
        super(account.name);
        mContext = context;
        mUserInfo = userInfo;
        mAccount = account;
        mFragmentController = fragmentController;
    }

    @Override
    public void bindViewHolder(IconTextLineItem.ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.titleView.setText(mAccount.name);
    }

    @Override
    public void onClick(View view) {
        mFragmentController.launchFragment(
                AccountDetailsFragment.newInstance(mAccount, mUserInfo));
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
        AccountHelper accountHelper = new AccountHelper(mContext, mUserInfo.getUserHandle());
        Drawable picture = accountHelper.getDrawableForType(mContext, mAccount.type);

        if (picture != null) {
            iconView.setImageDrawable(picture);
        } else {
            // TODO: Have a new drawable for default account icon.
            iconView.setImageDrawable(mContext.getDrawable(R.drawable.ic_user));
        }
    }
}
