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

package com.android.car.settings.security;

import android.content.Intent;
import android.os.Bundle;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

/**
 * Give user choices of lock screen type: Pin/Pattern/Password or None.
 */
public class ChooseLockTypeFragment extends ListItemSettingsFragment {
    // Arbitrary request code for choose security lock activity.
    private static final int REQUEST_CHOOSE_LOCK = 10001;

    private ListItemProvider mItemProvider;

    public static ChooseLockTypeFragment newInstance() {
        ChooseLockTypeFragment chooseLockTypeFragment = new ChooseLockTypeFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.lock_settings_picker_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        chooseLockTypeFragment.setArguments(bundle);
        return chooseLockTypeFragment;
    }

    @Override
    public ListItemProvider getItemProvider() {
        if (mItemProvider == null) {
            mItemProvider = new ListItemProvider.ListProvider(getListItems());
        }
        return mItemProvider;
    }

    private List<ListItem> getListItems() {
        List<ListItem> items = new ArrayList<>();
        items.add(createLockPatternLineItem());
        items.add(createLockPasswordLineItem());
        items.add(createLockPinLineItem());
        return items;
    }

    private ListItem createLockPatternLineItem() {
        TextListItem item = new TextListItem(getContext());
        item.setTitle(getString(R.string.security_lock_pattern));
        item.setOnClickListener(view -> startChooseLockPatternActivity());
        return item;
    }

    private ListItem createLockPasswordLineItem() {
        TextListItem item = new TextListItem(getContext());
        item.setTitle(getString(R.string.security_lock_password));
        item.setOnClickListener(view -> startChooseLockPasswordActivity());
        return item;
    }

    private ListItem createLockPinLineItem() {
        TextListItem item = new TextListItem(getContext());
        item.setTitle(getString(R.string.security_lock_pin));
        item.setOnClickListener(view -> startChooseLockPinActivity());
        return item;
    }

    private void startChooseLockPatternActivity() {
        Intent intent = new Intent(getContext(), ChooseLockPatternActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_LOCK);
    }

    private void startChooseLockPasswordActivity() {
        Intent intent = new Intent(getContext(), ChooseLockPasswordActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_LOCK);
    }

    private void startChooseLockPinActivity() {
        Intent intent = new Intent(getContext(), ChooseLockPinActivity.class);
        startActivityForResult(intent, REQUEST_CHOOSE_LOCK);
    }
}
