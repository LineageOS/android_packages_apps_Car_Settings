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

package com.android.car.settings.datetime;

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;

import java.util.Calendar;

/**
 * A ListItem that displays and sets system date.
 */
class SetDateListItem extends TextListItem implements DatetimeSettingsFragment.ListRefreshObserver {

    private final Context mContext;
    private final FragmentController mFragmentController;

    SetDateListItem(Context context, FragmentController fragmentController) {
        super(context);
        mContext = context;
        mFragmentController = fragmentController;
        setTitle(context.getString(R.string.date_time_set_date));
        updateListItemData();
    }

    @Override
    public void onPreRefresh() {
        updateListItemData();
    }

    private void updateListItemData() {
        setBody(DateFormat.getLongDateFormat(mContext).format(Calendar.getInstance().getTime()));
        if (isEnabled()) {
            setSupplementalIcon(R.drawable.ic_chevron_right, /* showDivider= */ false);
            setOnClickListener(v ->
                    mFragmentController.launchFragment(new DatePickerFragment()));
        } else {
            setSupplementalIcon(null, /* showDivider= */ false);
            setOnClickListener(null);
        }
    }

    private boolean isEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME, 0) <= 0;
    }
}
