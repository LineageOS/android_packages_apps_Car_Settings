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
package com.android.car.settings.quicksettings;

import android.car.drivingstate.CarUxRestrictions;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.car.widget.PagedListView;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.CarUxRestrictionsHelper;
import com.android.car.settings.home.HomepageFragment;
import com.android.car.settings.users.UserIconProvider;
import com.android.car.settings.users.UsersListFragment;
import com.android.settingslib.users.UserManagerHelper;

/**
 * Shows a page to access frequently used settings.
 */
public class QuickSettingFragment extends BaseFragment {
    private static final String TAG = "QS";

    private static final float RESTRICTED_ALPHA = 0.5f;
    private static final float UNRESTRICTED_ALPHA = 1f;

    private UserManagerHelper  mUserManagerHelper;
    private QuickSettingGridAdapter mGridAdapter;
    private PagedListView mListView;
    private View mFullSettingBtn;
    private View mUserSwitcherBtn;

    /**
     * Returns an instance of this class.
     */
    public static QuickSettingFragment newInstance() {
        QuickSettingFragment quickSettingFragment = new QuickSettingFragment();
        Bundle bundle = quickSettingFragment.getBundle();
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_quick_settings);
        bundle.putInt(EXTRA_LAYOUT, R.layout.quick_settings);
        bundle.putInt(EXTRA_TITLE_ID, R.string.settings_label);
        quickSettingFragment.setArguments(bundle);
        return quickSettingFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().findViewById(R.id.action_bar_icon_container).setOnClickListener(
                v -> getActivity().finish());

        mUserManagerHelper = new UserManagerHelper(getContext());
        mListView = (PagedListView) getActivity().findViewById(R.id.list);
        mGridAdapter = new QuickSettingGridAdapter(getContext());
        mListView.getRecyclerView().setLayoutManager(mGridAdapter.getGridLayoutManager());

        mFullSettingBtn = getActivity().findViewById(R.id.full_setting_btn);
        mFullSettingBtn.setOnClickListener(v -> {
            mFragmentController.launchFragment(HomepageFragment.getInstance());
        });
        mUserSwitcherBtn = getActivity().findViewById(R.id.user_switcher_btn);
        mUserSwitcherBtn.setOnClickListener(v -> {
            mFragmentController.launchFragment(UsersListFragment.newInstance());
        });

        setupAccountButton();
        View exitBtn = getActivity().findViewById(R.id.exit_button);
        exitBtn.setOnClickListener(v -> mFragmentController.goBack());

        mGridAdapter
                .addTile(new WifiTile(getContext(), mGridAdapter, mFragmentController))
                .addTile(new BluetoothTile(getContext(), mGridAdapter))
                .addTile(new DayNightTile(getContext(), mGridAdapter))
                .addSeekbarTile(new BrightnessTile(getContext()));
        mListView.setAdapter(mGridAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mGridAdapter.stop();
    }

    private void setupAccountButton() {
        ImageView userIcon = (ImageView) getActivity().findViewById(R.id.user_icon);
        UserInfo currentUserInfo = mUserManagerHelper.getForegroundUserInfo();
        userIcon.setImageDrawable(
                UserIconProvider.getUserIcon(
                        currentUserInfo, mUserManagerHelper, getContext()));

        TextView userSwitcherText = (TextView) getActivity().findViewById(R.id.user_switcher_text);
        userSwitcherText.setText(currentUserInfo.name);
    }

    /**
     * Quick setting fragment is distraction optimized, so is allowed at all times.
     */
    @Override
    public boolean canBeShown(CarUxRestrictions carUxRestrictions) {
        return true;
    }

    @Override
    public void onUxRestrictionChanged(CarUxRestrictions carUxRestrictions) {
        // TODO: update tiles
        applyRestriction(CarUxRestrictionsHelper.isNoSetup(carUxRestrictions));
    }

    private void applyRestriction(boolean restricted) {
        mFullSettingBtn.setAlpha(restricted ? RESTRICTED_ALPHA : UNRESTRICTED_ALPHA);
    }
}
