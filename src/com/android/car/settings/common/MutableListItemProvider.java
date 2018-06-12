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
package com.android.car.settings.common;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;

import java.util.List;

/**
 * A wrapper around a list. The content can be swapped.
 *
 * @param <VH> class that extends {@link ListItem.ViewHolder}.
 */
public class MutableListItemProvider<VH extends ListItem.ViewHolder> extends ListItemProvider {
    private List<ListItem<VH>> mItems;

    public MutableListItemProvider(List<ListItem<VH>> items) {
        mItems = items;
    }

    public void setItems(List<ListItem<VH>> items) {
        mItems = items;
    }

    @Override
    public ListItem<VH> get(int position) {
        return mItems.get(position);
    }

    @Override
    public int size() {
        return mItems.size();
    }
}
