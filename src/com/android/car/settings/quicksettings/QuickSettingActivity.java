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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.car.widget.PagedListView;

import com.android.car.settings.R;
import com.android.car.settings.common.CarSettingActivity;

/**
 * Shows a page to access frequently used settings.
 */
public class QuickSettingActivity extends AppCompatActivity {
    private static final String TAG = "QS";

    private QuickSettingGridAdapter mGridAdapter;
    private PagedListView mListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quick_settings);

        mListView = (PagedListView) findViewById(R.id.list);
        mGridAdapter = new QuickSettingGridAdapter(this);
        mListView.getRecyclerView().setLayoutManager(mGridAdapter.getGridLayoutManager());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // make the toolbar take the whole width.
        toolbar.setPadding(0, 0, 0, 0);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setCustomView(R.layout.action_bar_with_button);
        actionBar.setDisplayShowCustomEnabled(true);

        Button adavancedSettingBtn = (Button) findViewById(R.id.action_button1);
        Button userSwitcherBtn = (Button) findViewById(R.id.action_button2);
        adavancedSettingBtn.setText(R.string.advanced_settings_label);
        adavancedSettingBtn.setVisibility(View.VISIBLE);
        adavancedSettingBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CarSettingActivity.class);
            startActivity(intent);
        });

        userSwitcherBtn.setText(R.string.user_and_account_settings_title);
        userSwitcherBtn.setVisibility(View.VISIBLE);
        userSwitcherBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CarSettingActivity.class);
            intent.setAction(CarSettingActivity.ACTION_LIST_USER);
            startActivity(intent);
        });
        View exitBtn = findViewById(R.id.back_button);
        ((ImageView) exitBtn).setImageResource(R.drawable.ic_close);
        exitBtn.setOnClickListener(v -> finish());
    }

    @Override
    public void onStart() {
        super.onStart();

        mGridAdapter
                .addTile(new WifiTile(this, mGridAdapter))
                .addTile(new BluetoothTile(this, mGridAdapter))
                .addTile(new DayNightTile(this, mGridAdapter))
                .addSeekbarTile(new BrightnessTile(this));
        mListView.setAdapter(mGridAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mGridAdapter.stop();
    }
}
