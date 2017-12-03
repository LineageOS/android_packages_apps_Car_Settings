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

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.EXTRA_USER;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.android.car.list.IconTextLineItem;
import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

/**
 * Builds the account type list, as a follow up of Add account.
 */
public class AccountTypeLineItem extends IconTextLineItem {
    private final Context mContext;
    private final UserInfo mUserInfo;
    private final String mProviderName;
    private final String mAccountType;

    public AccountTypeLineItem(
            @NonNull Context context,
            UserInfo userInfo,
            String providerName,
            String accountType) {
        super(providerName);
        mContext = context;
        mUserInfo = userInfo;
        mProviderName = providerName;
        mAccountType = accountType;
    }

    @Override
    public void bindViewHolder(IconTextLineItem.ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.titleView.setText(mProviderName);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), AddAccountActivity.class);
        intent.putExtra(AddAccountActivity.EXTRA_SELECTED_ACCOUNT, mAccountType);
        view.getContext().startActivity(intent);
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
        AuthHelper authHelper = new AuthHelper(mContext, mUserInfo.getUserHandle());
        Drawable picture = authHelper.getDrawableForType(mContext, mAccountType);

        if (picture != null) {
            iconView.setImageDrawable(picture);
        } else {
            // TODO: Have a new drawable for default account icon.
            iconView.setImageDrawable(mContext.getDrawable(R.drawable.ic_user));
        }
    }
}
