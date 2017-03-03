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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;

import android.support.car.ui.PagedListView;
import android.support.v7.widget.RecyclerView;
import com.android.car.settings.CarSettingActivity;
import com.android.car.settings.R;
import com.android.car.settings.common.NoDividerItemDecoration;
import com.android.car.settings.common.TypedPagedListAdapter;
import com.android.settingslib.datetime.ZoneGetter;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Configures date time
 */
public class DatetimeSettingsActivity extends CarSettingActivity {
    private static final String TAG = "DatetimeSettingsActivity";

    private static final IntentFilter TIME_CHANGED_FILTER =
            new IntentFilter(Intent.ACTION_TIME_CHANGED);

    // Minimum time is Nov 5, 2007, 0:00.
    public static final long MIN_DATE = 1194220800000L;

    private final TimeChangedBroadCastReceiver mTimeChangedBroadCastReceiver =
            new TimeChangedBroadCastReceiver();

    private PagedListView mListView;
    private TypedPagedListAdapter mPagedListAdapter;
    private final ArrayList<TypedPagedListAdapter.LineItem> mLineItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showMenuIcon();
        setContentView(R.layout.paged_list);

        mLineItems.add(new DateTimeToggleLineItem(this,
                getString(R.string.date_time_auto),
                getString(R.string.date_time_auto_summary),
                Settings.Global.AUTO_TIME));
        mLineItems.add(new DateTimeToggleLineItem(this,
                getString(R.string.zone_auto),
                getString(R.string.zone_auto_summary),
                Settings.Global.AUTO_TIME_ZONE));
        mLineItems.add(new SetDateLineItem(this));
        mLineItems.add(new SetTimeLineItem(this));
        mLineItems.add(new SetTimeZoneLineItem(this));
        mLineItems.add(new TimeFormatToggleLineItem(this));

        mListView = (PagedListView) findViewById(android.R.id.list);
        mListView.setDefaultItemDecoration(new NoDividerItemDecoration(this));
        mListView.setDarkMode();
        mPagedListAdapter = new TypedPagedListAdapter(this /* context */, mLineItems);
        mListView.setAdapter(mPagedListAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(mTimeChangedBroadCastReceiver, TIME_CHANGED_FILTER);
        mPagedListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(mTimeChangedBroadCastReceiver);
    }

    private class TimeChangedBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPagedListAdapter.notifyDataSetChanged();
        }
    }
}
