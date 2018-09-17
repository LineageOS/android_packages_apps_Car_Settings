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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.common.ListItemSettingsFragment;

import java.util.ArrayList;

/**
 * Fragment showing legal information.
 */
public class LegalInformationFragment extends ListItemSettingsFragment {
    private static final String ACTION_WEBVIEW_LICENSE = "android.settings.WEBVIEW_LICENSE";

    /**
     * Factory method for creating the fragment.
     */
    public static LegalInformationFragment newInstance() {
        LegalInformationFragment fragment = new LegalInformationFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.legal_information);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public ListItemProvider getItemProvider() {
        return new ListItemProvider.ListProvider(getListItems());
    }

    private ArrayList<ListItem> getListItems() {
        ArrayList<ListItem> listItems = new ArrayList<>();

        listItems.add(createSystemWebviewLicensesListItem());
        listItems.add(createThirdPartyLicensesListItem());

        return listItems;
    }

    private TextListItem createSystemWebviewLicensesListItem() {
        Context context = requireContext();
        return createSimpleListItem(R.string.webview_license_title, v -> {
            Intent intent = new Intent();
            intent.setAction(ACTION_WEBVIEW_LICENSE);
            context.startActivity(intent);
        });
    }

    private TextListItem createThirdPartyLicensesListItem() {
        Context context = requireContext();
        return createSimpleListItem(R.string.settings_license_activity_title, v -> {
            Intent intent = new Intent(context, ThirdPartyLicensesActivity.class);
            context.startActivity(intent);
        });
    }
}
