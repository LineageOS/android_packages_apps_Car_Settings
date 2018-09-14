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

package com.android.car.settings.datetime;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.settingslib.datetime.ZoneGetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lists all time zone and its offset from GMT.
 */
public class TimeZonePickerFragment extends ListItemSettingsFragment implements
        TimeZoneListItem.TimeZoneChangeListener {
    private List<Map<String, Object>> mZoneList;

    @Override
    @StringRes
    protected int getTitleId() {
        return R.string.date_time_set_timezone_title;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mZoneList = ZoneGetter.getZonesList(getContext());
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public ListItemProvider getItemProvider() {
        return new ListItemProvider.ListProvider(getListItems());
    }

    private List<ListItem> getListItems() {
        List<ListItem> listItems = new ArrayList<>();
        for (Map<String, Object> zone : mZoneList) {
            listItems.add(new TimeZoneListItem(getContext(), this, zone));
        }
        return listItems;
    }

    @Override
    public void onTimeZoneChanged() {
        getContext().sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
        getFragmentController().goBack();
    }
}
