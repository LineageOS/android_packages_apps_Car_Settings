/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.car.settings.qc;

import static android.car.settings.CarSettings.Global.FORCED_DAY_NIGHT_MODE;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.io.IOException;

/**
 * Background worker for the {@link ThemeToggle} QCItem.
 */
public final class ThemeToggleWorker extends SettingsQCBackgroundWorker<ThemeToggle> {
    private boolean mContentObserverRegistered;
    private final ContentObserver mForcedNightModeObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    notifyQCItemChange();
                }
            };

    public ThemeToggleWorker(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected void onQCItemSubscribe() {
        if (!mContentObserverRegistered) {
            getContext().getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(FORCED_DAY_NIGHT_MODE),
                    false /* notifyForDescendants */,
                    mForcedNightModeObserver);
            mContentObserverRegistered = true;
        }
    }

    @Override
    protected void onQCItemUnsubscribe() {
        unregisterContentObserver();
    }

    @Override
    public void close() throws IOException {
        unregisterContentObserver();
    }

    private void unregisterContentObserver() {
        if (mContentObserverRegistered) {
            getContext().getContentResolver().unregisterContentObserver(mForcedNightModeObserver);
            mContentObserverRegistered = false;
        }
    }
}
