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

package com.android.car.settings.home;

import static com.android.car.settings.common.ExtraSettingsLoader.DEVICE_CATEGORY;
import static com.android.car.settings.common.ExtraSettingsLoader.PERSONAL_CATEGORY;
import static com.android.car.settings.common.ExtraSettingsLoader.WIRELESS_CATEGORY;

import android.car.user.CarUserManagerHelper;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;
import androidx.loader.app.LoaderManager;

import com.android.car.settings.R;
import com.android.car.settings.accounts.AccountsListFragment;
import com.android.car.settings.applications.ApplicationSettingsFragment;
import com.android.car.settings.bluetooth.BluetoothSettingsFragment;
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
import com.android.car.settings.wifi.WifiSettingsFragment;
import com.android.car.settings.wifi.WifiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Homepage for settings for car.
 */
public class HomepageFragment extends ListItemSettingsFragment implements
        SettingsSuggestionsController.Listener {
    private static final Logger LOG = new Logger(HomepageFragment.class);

    private SettingsSuggestionsController mSettingsSuggestionsController;
    private CarUserManagerHelper mCarUserManagerHelper;
    // This tracks the number of suggestions currently shown in the fragment. This is based off of
    // the assumption that suggestions are 0 through (num suggestions - 1) in the adapter. Do not
    // change this assumption without updating the code in onSuggestionLoaded.
    private int mNumSettingsSuggestions;

    private List<ListItem> mListItems;

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
                        context,
                        LoaderManager.getInstance(/* owner= */ this),
                        /* listener= */ this);
        mCarUserManagerHelper = new CarUserManagerHelper(context);
        mListItems = getListItems();

        // reset the suggestion count.
        mNumSettingsSuggestions = 0;
    }

    @Override
    public void onStart() {
        super.onStart();
        mSettingsSuggestionsController.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mSettingsSuggestionsController.stop();
    }

    @Override
    public ListItemProvider getItemProvider() {
        return new ListItemProvider.ListProvider(mListItems);
    }

    private ArrayList<ListItem> getListItems() {
        ExtraSettingsLoader extraSettingsLoader = new ExtraSettingsLoader(getContext());
        Map<String, Collection<ListItem>> extraSettings =
                extraSettingsLoader.load();
        ArrayList<ListItem> listItems = new ArrayList<>();

        listItems.add(
                createFragmentListItem(R.string.display_settings, R.drawable.ic_settings_display,
                        DisplaySettingsFragment.newInstance()));
        listItems.add(createFragmentListItem(R.string.sound_settings, R.drawable.ic_settings_sound,
                SoundSettingsFragment.newInstance()));
        if (WifiUtil.isWifiAvailable(requireContext())) {
            listItems.add(
                    createFragmentListItem(R.string.wifi_settings, R.drawable.ic_settings_wifi,
                            WifiSettingsFragment.newInstance()));
        }
        listItems.addAll(extraSettings.get(WIRELESS_CATEGORY));
        listItems.add(createFragmentListItem(R.string.bluetooth_settings,
                R.drawable.ic_settings_bluetooth, BluetoothSettingsFragment.getInstance()));
        listItems.add(createFragmentListItem(R.string.applications_settings,
                R.drawable.ic_settings_applications, ApplicationSettingsFragment.newInstance()));
        listItems.add(createFragmentListItem(R.string.date_and_time_settings_title,
                R.drawable.ic_settings_date_time, DatetimeSettingsFragment.getInstance()));
        listItems.add(createFragmentListItem(R.string.users_list_title, R.drawable.ic_user,
                getUserManagementFragment()));
        // Guest users can't set screen locks or add/remove accounts.
        if (!mCarUserManagerHelper.isCurrentProcessGuestUser()) {
            listItems.add(
                    createFragmentListItem(R.string.accounts_settings_title, R.drawable.ic_account,
                            AccountsListFragment.newInstance()));
            listItems.add(createListItem(R.string.security_settings_title, R.drawable.ic_lock,
                    v -> startActivity(
                            new Intent(requireContext(), SettingsScreenLockActivity.class))));
        }
        listItems.add(
                createFragmentListItem(R.string.system_setting_title, R.drawable.ic_settings_about,
                        SystemSettingsFragment.getInstance()));
        listItems.addAll(extraSettings.get(DEVICE_CATEGORY));
        listItems.addAll(extraSettings.get(PERSONAL_CATEGORY));
        return listItems;
    }

    private TextListItem createFragmentListItem(@StringRes int titleId, @DrawableRes int iconRes,
            BaseFragment fragment) {
        return createListItem(titleId, iconRes,
                v -> getFragmentController().launchFragment(fragment));
    }

    private TextListItem createListItem(@StringRes int titleId, @DrawableRes int iconRes,
            View.OnClickListener onClickListener) {
        Context context = requireContext();
        TextListItem item = new TextListItem(context);
        item.setTitle(context.getString(titleId));
        item.setPrimaryActionIcon(iconRes, TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item.setSupplementalIcon(R.drawable.ic_chevron_right, /* showDivider= */ false);
        item.setOnClickListener(onClickListener);
        return item;
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
    public void onSuggestionsLoaded(@NonNull ArrayList<ListItem> suggestions) {
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
