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
 * limitations under the License
 */
package com.android.car.settings.quicksettings;


import android.annotation.DrawableRes;
import android.annotation.Nullable;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telephony.TelephonyManager;
import android.view.View;

import com.android.car.settings.R;
import com.android.settingslib.net.DataUsageController;

/**
 * Controls celluar on quick setting page.
 */
public class CelluarTile implements QuickSettingGridAdapter.Tile, DataUsageController.Callback{
    private final Context mContext;
    private final StateChangedListener mStateChangedListener;
    private final DataUsageController mDataUsageController;
    private final String mCarrierName;

    @DrawableRes
    private int mIconOnRes = R.drawable.ic_data_on;
    @DrawableRes
    private int mIconOffRes = R.drawable.ic_data_off;

    private State mState = State.ON;

    CelluarTile(Context context, StateChangedListener stateChangedListener) {
        mStateChangedListener = stateChangedListener;
        mContext = context;
        TelephonyManager manager =
                (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mDataUsageController = new DataUsageController(mContext);
        mCarrierName = mDataUsageController.isMobileDataSupported()
                ? manager.getNetworkOperatorName() : "Cell Data";
        mState = mDataUsageController.isMobileDataSupported()
                && mDataUsageController.isMobileDataEnabled() ? State.ON : State.OFF;
    }

    @Override
    public void onMobileDataEnabled(boolean enabled) {
        mState = enabled ? State.ON : State.OFF;
    }

    @Override
    public Drawable getIcon() {
        return mContext.getDrawable(mState == State.ON ? mIconOnRes : mIconOffRes);
    }

    @Override
    @Nullable
    public String getText() {
        return mCarrierName;
    }

    @Override
    public State getState() {
        return mState;
    }

    @Override
    public void stop() {
    }

    @Override
    public void onClick(View v) {
        mDataUsageController.setMobileDataEnabled(!mDataUsageController.isMobileDataEnabled());
    }
}
