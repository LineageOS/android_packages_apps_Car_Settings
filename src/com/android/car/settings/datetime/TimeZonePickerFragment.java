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
package com.android.car.settings.datetime;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.R;
import com.android.car.view.PagedListView;

/**
 * Lists all time zone and its offset from GMT.
 */
public class TimeZonePickerFragment extends BaseFragment implements
        TimeZoneListAdapter.TimeZoneChangeListener {

    public static TimeZonePickerFragment getInstance() {
        TimeZonePickerFragment timeZonePickerFragment = new TimeZonePickerFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.date_time_set_timezone_title);
        bundle.putInt(EXTRA_LAYOUT, R.layout.list);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar);
        timeZonePickerFragment.setArguments(bundle);
        return timeZonePickerFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PagedListView listView = (PagedListView) getView().findViewById(R.id.list);
        listView.setDefaultItemDecoration(new PagedListView.Decoration(getContext()));
        listView.setDarkMode();
        TimeZoneListAdapter adapter = new TimeZoneListAdapter(
                getContext(), this /* TimeZoneChangeListener */);
        listView.setAdapter(adapter);
    }

    @Override
    public void onTimeZoneChanged() {
        getContext().sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));
        mFragmentController.goBack();
    }
}
