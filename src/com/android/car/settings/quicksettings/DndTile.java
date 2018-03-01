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

import static android.provider.Settings.Global.ZEN_MODE_OFF;
import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;

import android.annotation.DrawableRes;
import android.annotation.Nullable;
import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.View;

import com.android.car.settings.R;
import com.android.settingslib.net.DataUsageController;

/**
 * Controls Do Not Disturb or Zen mode on quick setting page.
 */
public class DndTile extends ContentObserver implements QuickSettingGridAdapter.Tile {
    private static final String TAG = "DndTile";
    private final Context mContext;
    private final StateChangedListener mStateChangedListener;
    private final NotificationManager mNotificationManager;

    @DrawableRes
    private int mIconOnRes = R.drawable.ic_dnd_on;
    @DrawableRes
    private int mIconOffRes = R.drawable.ic_dnd_off;

    private State mState;

    DndTile(Context context, StateChangedListener stateChangedListener, Handler handler) {
        super(handler);
        mStateChangedListener = stateChangedListener;
        mContext = context;
        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mState = mNotificationManager.getZenMode() != ZEN_MODE_OFF ? State.ON : State.OFF;

        mContext.getContentResolver().registerContentObserver(
            Global.getUriFor(Global.ZEN_MODE), false, this);
    }

    @Override
    public void onChange(boolean selfChange) {
        Log.w(TAG, "Zen mode changed.");
        mState = mNotificationManager.getZenMode() != ZEN_MODE_OFF ? State.ON : State.OFF;
    }

    @Override
    public Drawable getIcon() {
        return mContext.getDrawable(mState == State.ON ? mIconOnRes : mIconOffRes);
    }

    @Override
    @Nullable
    public String getText() {
        return "No not disturb";
    }

    @Override
    public State getState() {
        return mState;
    }

    @Override
    public void stop() {
        mContext.getContentResolver().unregisterContentObserver(this);
    }

    @Override
    public void onClick(View v) {
        if (mNotificationManager.getZenMode() != ZEN_MODE_OFF) {
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG);
        } else {
            mNotificationManager.setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG);
        }
    }
}
