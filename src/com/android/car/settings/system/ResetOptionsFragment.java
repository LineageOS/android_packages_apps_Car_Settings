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

package com.android.car.settings.system;

import android.car.user.CarUserManagerHelper;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;

import java.util.ArrayList;

/**
 * Shows options to reset network settings, reset app preferences, and factory reset the device.
 */
public class ResetOptionsFragment extends ListItemSettingsFragment {

    private CarUserManagerHelper mCarUserManagerHelper;
    private ListItemProvider mItemProvider;

    /**
     * Factory method for creating LanguagePickerFragment.
     */
    public static ResetOptionsFragment newInstance() {
        ResetOptionsFragment fragment = new ResetOptionsFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.reset_options_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCarUserManagerHelper = new CarUserManagerHelper(context);
        mItemProvider = new ListItemProvider.ListProvider(getListItems());
    }

    @Override
    public ListItemProvider getItemProvider() {
        return mItemProvider;
    }

    private ArrayList<ListItem> getListItems() {
        boolean isAdmin = mCarUserManagerHelper.isCurrentProcessAdminUser();

        ArrayList<ListItem> lineItems = new ArrayList<>();

        if (isAdmin && !mCarUserManagerHelper.isCurrentProcessUserHasRestriction(
                UserManager.DISALLOW_NETWORK_RESET)) {
            lineItems.add(createListItem(R.string.reset_network_title, v ->
                    getFragmentController().launchFragment(ResetNetworkFragment.newInstance())
            ));
        }
        lineItems.add(createListItem(R.string.reset_app_preferences_title, v -> {
            // TODO: launch reset app preferences.
        }));
        if (isAdmin && !mCarUserManagerHelper.isCurrentProcessUserHasRestriction(
                UserManager.DISALLOW_FACTORY_RESET)) {
            lineItems.add(createListItem(R.string.master_clear_title, v -> {
                // TODO: launch master clear.
            }));
        }

        return lineItems;
    }

    private TextListItem createListItem(@StringRes int titleResId,
            View.OnClickListener onClickListener) {
        Context context = getContext();
        TextListItem item = new TextListItem(context);
        item.setTitle(context.getString(titleResId));
        item.setSupplementalIcon(R.drawable.ic_chevron_right, /* showDivider= */ false);
        item.setOnClickListener(onClickListener);
        return item;
    }
}
