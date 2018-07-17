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

import static android.app.Activity.RESULT_OK;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.StringRes;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.security.CheckLockActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Presents the user with information about restoring network settings to the factory default
 * values. If a user confirms, they will first be required to authenticate then presented with a
 * secondary confirmation: {@link ResetNetworkConfirmFragment}.
 */
public class ResetNetworkFragment extends ListItemSettingsFragment {

    private static final int REQUEST_CODE = 123;

    /**
     * Creates new instance of {@link ResetNetworkFragment}.
     */
    public static ResetNetworkFragment newInstance() {
        ResetNetworkFragment fragment = new ResetNetworkFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.reset_network_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Button resetSettingsButton = requireNonNull(getActivity()).findViewById(
                R.id.action_button1);
        resetSettingsButton.setText(getContext().getString(R.string.reset_network_button_text));
        resetSettingsButton.setOnClickListener(v -> startActivityForResult(new Intent(
                getContext(), CheckLockActivity.class), REQUEST_CODE));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            getFragmentController().launchFragment(ResetNetworkConfirmFragment.newInstance());
        }
    }

    @Override
    public ListItemProvider getItemProvider() {
        return new ListItemProvider.ListProvider(getListItems());
    }

    private List<ListItem> getListItems() {
        List<ListItem> items = new ArrayList<>();
        items.add(createTextOnlyItem(R.string.reset_network_desc));
        items.add(createTextOnlyItem(R.string.reset_network_items));
        return items;
    }

    private TextListItem createTextOnlyItem(@StringRes int stringResId) {
        Context context = requireContext();
        TextListItem item = new TextListItem(context);
        item.setBody(context.getString(stringResId), /* asPrimary= */ true);
        item.setHideDivider(true);
        return item;
    }
}
