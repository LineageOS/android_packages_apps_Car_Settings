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
package com.android.car.settings.quicksettings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;
import java.util.ArrayList;

import com.android.car.list.TypedPagedListAdapter;
import com.android.car.settings.R;
import com.android.car.settings.common.CarSettingActivity;

import androidx.car.widget.PagedListView;
import com.android.car.settings.display.BrightnessLineItem;

/**
 * Shows a page to access frequently used settings.
 */
public class QuickSettingActivity extends AppCompatActivity {
    private static final String TAG = "QS";

    private QuickSettingGridAdapter mGridAdapter;
    private TypedPagedListAdapter mPagedListAdapter;
    private GridView mGridView;
    private PagedListView mListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quick_settings);

        mGridView = (GridView) findViewById(R.id.grid);
        mListView = (PagedListView) findViewById(R.id.list);

        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.setting_icon).setOnClickListener(v -> {
            Intent intent = new Intent(this, CarSettingActivity.class);
            startActivity(intent);
        } );
    }

    @Override
    public void onStart() {
        super.onStart();
        mGridAdapter = new QuickSettingGridAdapter(this);
        WifiTile wifiTile = new WifiTile(this, mGridAdapter);
        BluetoothTile bluetoothTile = new BluetoothTile(this, mGridAdapter);
        DayNightTile dayNightTile = new DayNightTile(this, mGridAdapter);
        mGridAdapter.addTile(wifiTile);
        mGridAdapter.addTile(bluetoothTile);
        mGridAdapter.addTile(dayNightTile);
        mGridView.setAdapter(mGridAdapter);
        ArrayList<TypedPagedListAdapter.LineItem> lineItems = new ArrayList<>();
        lineItems.add(new BrightnessLineItem(this));
        mPagedListAdapter = new TypedPagedListAdapter(lineItems);
        mListView.setAdapter(mPagedListAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mGridAdapter.stop();
    }
}
