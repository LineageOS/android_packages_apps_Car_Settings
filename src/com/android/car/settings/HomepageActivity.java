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
 * limitations under the License.
 */
package com.android.car.settings;


import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import com.android.car.settings.common.TileRecyclerViewAdapter;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.CategoryManager;

/**
 * Homepage for settings for car.
 */
public class HomepageActivity extends CarSettingActivity {

    private RecyclerView mRecyclerView;
    private TileRecyclerViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.homepage);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new TileRecyclerViewAdapter(this, CategoryManager.get(this)
                .getTilesByCategory(this, CategoryKey.CATEGORY_HOMEPAGE, getSettingPkg()));
        mRecyclerView.setAdapter(mAdapter);
    }
}
