/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.io.IOException;

/**
 * Base background worker for mobile data QCItems.
 * @param <E> The {@link SettingsQCItem} the background worker is associated with.
 */
public abstract class MobileDataBaseWorker<E extends SettingsQCItem>
        extends SettingsQCBackgroundWorker<E> {

    private final int mSubId;
    private final ContentObserver mMobileDataChangeObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            notifyQCItemChange();
        }
    };

    protected MobileDataBaseWorker(Context context, Uri uri) {
        super(context, uri);
        mSubId = SubscriptionManager.getDefaultDataSubscriptionId();
    }

    @Override
    protected void onQCItemSubscribe() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            getContext().getContentResolver().registerContentObserver(getObservableUri(mSubId),
                    /* notifyForDescendants= */ false, mMobileDataChangeObserver);
        }
    }

    @Override
    protected void onQCItemUnsubscribe() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            getContext().getContentResolver().unregisterContentObserver(mMobileDataChangeObserver);
        }
    }

    @Override
    public void close() throws IOException {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            getContext().getContentResolver().unregisterContentObserver(mMobileDataChangeObserver);
        }
    }

    private Uri getObservableUri(int subId) {
        Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA);
        if (TelephonyManager.from(getContext()).getSimCount() != 1) {
            uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId);
        }
        return uri;
    }
}
