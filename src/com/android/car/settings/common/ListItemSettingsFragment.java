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

package com.android.car.settings.common;

import android.os.Bundle;

import com.android.car.settings.R;

import java.util.List;

import androidx.car.widget.DayNightStyle;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider.ListProvider;
import androidx.car.widget.PagedListView;

/**
 * Settings page that only contain a list of items.
 * <p>
 * Uses support library ListItemAdapter, unlike ListSettingsFragment that uses the car-list
 * lists.
 */
public abstract class ListItemSettingsFragment extends BaseFragment {
    private ListItemAdapter mListAdapter;
    private ListProvider mItemProvider;

    /**
     * Gets bundle adding the list_fragment layout to it.
     */
    protected static Bundle getBundle() {
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_LAYOUT, R.layout.list_fragment);
        return bundle;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mItemProvider = new ListProvider(getListItems());
        mListAdapter = new ListItemAdapter(getContext(), mItemProvider);

        PagedListView listView = getView().findViewById(R.id.list);
        listView.setDayNightStyle(DayNightStyle.FORCE_DAY);
        listView.setAdapter(mListAdapter);
        listView.setDividerVisibilityManager(mListAdapter);
    }

    /**
     * Called in onActivityCreated.
     * Gets ListItems that should show up in the list.
     */
    public abstract List<ListItem> getListItems();
}
