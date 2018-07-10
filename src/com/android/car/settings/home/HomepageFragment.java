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
package com.android.car.settings.home;


import static com.android.car.settings.common.ExtraSettingsLoader.DEVICE_CATEGORY;
import static com.android.car.settings.common.ExtraSettingsLoader.PERSONAL_CATEGORY;
import static com.android.car.settings.common.ExtraSettingsLoader.WIRELESS_CATEGORY;

import android.bluetooth.BluetoothAdapter;
import android.car.user.CarUserManagerHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;
import com.android.car.settings.accounts.AccountsListFragment;
import com.android.car.settings.applications.ApplicationSettingsFragment;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.ExtraSettingsLoader;
import com.android.car.settings.common.ListItemSettingsFragment;
import com.android.car.settings.common.Logger;
import com.android.car.settings.datetime.DatetimeSettingsFragment;
import com.android.car.settings.display.DisplaySettingsFragment;
import com.android.car.settings.security.SettingsScreenLockActivity;
import com.android.car.settings.sound.SoundSettingsFragment;
import com.android.car.settings.suggestions.SettingsSuggestionsController;
import com.android.car.settings.system.SystemSettingsFragment;
import com.android.car.settings.users.UserDetailsFragment;
import com.android.car.settings.users.UsersListFragment;
import com.android.car.settings.wifi.CarWifiManager;
import com.android.car.settings.wifi.WifiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Homepage for settings for car.
 */
public class HomepageFragment extends ListItemSettingsFragment implements
        CarWifiManager.Listener, SettingsSuggestionsController.Listener {
    private static final Logger LOG = new Logger(HomepageFragment.class);

    private SettingsSuggestionsController mSettingsSuggestionsController;
    private CarWifiManager mCarWifiManager;
    private WifiLineItem mWifiLineItem;
    private BluetoothLineItem mBluetoothLineItem;
    private CarUserManagerHelper mCarUserManagerHelper;
    // This tracks the number of suggestions currently shown in the fragment. This is based off of
    // the assumption that suggestions are 0 through (num suggestions - 1) in the adapter. Do not
    // change this assumption without updating the code in onSuggestionLoaded.
    private int mNumSettingsSuggestions;

    private List<ListItem> mListItems;
    private final BroadcastReceiver mBtStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        // TODO show a different status icon?
                    case BluetoothAdapter.STATE_OFF:
                        mBluetoothLineItem.onBluetoothStateChanged(false);
                        refreshList();
                        break;
                    default:
                        mBluetoothLineItem.onBluetoothStateChanged(true);
                        refreshList();
                }
            }
        }
    };

    private final IntentFilter mBtStateChangeFilter =
            new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

    /**
     * Gets an instance of this class.
     */
    public static HomepageFragment newInstance() {
        HomepageFragment homepageFragment = new HomepageFragment();
        Bundle bundle = ListItemSettingsFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.settings_label);
        homepageFragment.setArguments(bundle);
        return homepageFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSettingsSuggestionsController =
                new SettingsSuggestionsController(
                        getContext(),
                        getLoaderManager(),
                        /* listener= */ this);
        mCarWifiManager = new CarWifiManager(getContext(), /* listener= */ this);
        if (WifiUtil.isWifiAvailable(getContext())) {
            mWifiLineItem = new WifiLineItem(
                    getContext(), mCarWifiManager, getFragmentController(),
                    /* listController= */ this);
        }
        mBluetoothLineItem = new BluetoothLineItem(getContext(), getFragmentController());
        mCarUserManagerHelper = new CarUserManagerHelper(getContext());
        mListItems = getListItems();

        // reset the suggestion count.
        mNumSettingsSuggestions = 0;
    }

    @Override
    public void onAccessPointsChanged() {
        // don't care
    }

    @Override
    public void onWifiStateChanged(int state) {
        mWifiLineItem.onWifiStateChanged(state);
    }

    @Override
    public void onStart() {
        super.onStart();
        mCarWifiManager.start();
        mSettingsSuggestionsController.start();
        getActivity().registerReceiver(mBtStateReceiver, mBtStateChangeFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mCarWifiManager.stop();
        mSettingsSuggestionsController.stop();
        getActivity().unregisterReceiver(mBtStateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCarWifiManager.destroy();
    }

    @Override
    public ListItemProvider getItemProvider() {
        return new ListItemProvider.ListProvider(mListItems);
    }

    private ArrayList<ListItem> getListItems() {
        ExtraSettingsLoader extraSettingsLoader = new ExtraSettingsLoader(getContext());
        Map<String, Collection<ListItem>> extraSettings =
                extraSettingsLoader.load();
        ArrayList<ListItem> lineItems = new ArrayList<>();

        lineItems.add(new SimpleIconTransitionLineItem(
                R.string.display_settings,
                R.drawable.ic_settings_display,
                getContext(),
                null,
                DisplaySettingsFragment.newInstance(),
                getFragmentController()));
        lineItems.add(new SimpleIconTransitionLineItem(
                R.string.sound_settings,
                R.drawable.ic_settings_sound,
                getContext(),
                null,
                SoundSettingsFragment.newInstance(),
                getFragmentController()));
        if (mWifiLineItem != null) {
            lineItems.add(mWifiLineItem);
        }
        lineItems.addAll(extraSettings.get(WIRELESS_CATEGORY));
        lineItems.add(mBluetoothLineItem);
        lineItems.add(new SimpleIconTransitionLineItem(
                R.string.applications_settings,
                R.drawable.ic_settings_applications,
                getContext(),
                null,
                ApplicationSettingsFragment.newInstance(),
                getFragmentController()));
        lineItems.add(new SimpleIconTransitionLineItem(
                R.string.date_and_time_settings_title,
                R.drawable.ic_settings_date_time,
                getContext(),
                null,
                DatetimeSettingsFragment.getInstance(),
                getFragmentController()));
        lineItems.add(new SimpleIconTransitionLineItem(
                R.string.users_list_title,
                R.drawable.ic_user,
                getContext(),
                null,
                getUserManagementFragment(),
                getFragmentController()));

        // Guest users can't set screen locks or add/remove accounts.
        if (!mCarUserManagerHelper.isCurrentProcessGuestUser()) {

            lineItems.add(new SimpleIconTransitionLineItem(
                    R.string.accounts_settings_title,
                    R.drawable.ic_account,
                    getContext(),
                    null,
                    AccountsListFragment.newInstance(),
                    getFragmentController()));

            TextListItem item = new TextListItem(getContext());
            item.setTitle(getString(R.string.security_settings_title));
            item.setPrimaryActionIcon(R.drawable.ic_lock, /* useLargeIcon= */ false);
            item.setSupplementalIcon(R.drawable.ic_chevron_right, /* showDivider= */ false);
            item.setOnClickListener(v -> startActivity(new Intent(
                    getContext(), SettingsScreenLockActivity.class)));

            lineItems.add(item);
        }

        lineItems.add(new SimpleIconTransitionLineItem(
                R.string.system_setting_title,
                R.drawable.ic_settings_about,
                getContext(),
                null,
                SystemSettingsFragment.getInstance(),
                getFragmentController()));

        lineItems.addAll(extraSettings.get(DEVICE_CATEGORY));
        lineItems.addAll(extraSettings.get(PERSONAL_CATEGORY));
        return lineItems;
    }

    private BaseFragment getUserManagementFragment() {
        if (mCarUserManagerHelper.isCurrentProcessAdminUser()) {
            // Admins can see a full list of users in Settings.
            LOG.v("getUserManagementFragment Creating UsersListFragment for admin user.");
            return UsersListFragment.newInstance();
        }
        // Non-admins can only manage themselves in Settings.
        LOG.v("getUserManagementFragment Creating UserDetailsFragment for non-admin.");
        return UserDetailsFragment.newInstance(mCarUserManagerHelper.getCurrentProcessUserId());
    }

    @Override
    public void onSuggestionsLoaded(ArrayList<ListItem> suggestions) {
        LOG.v("onDeferredSuggestionsLoaded");
        // TODO: there's two loops here to remove and add suggestions, there are some room to
        // improve the efficiency.
        mListItems.subList(0, mNumSettingsSuggestions).clear();
        mNumSettingsSuggestions = suggestions.size();
        mListItems.addAll(0, suggestions);
        refreshList();
    }

    @Override
    public void onSuggestionDismissed(int adapterPosition) {
        LOG.v("onSuggestionDismissed adapterPosition " + adapterPosition);
        mListItems.remove(adapterPosition);
        refreshList();
    }
}
