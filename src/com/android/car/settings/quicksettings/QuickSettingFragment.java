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

import android.app.Activity;
import android.car.drivingstate.CarUxRestrictions;
import android.car.user.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.car.widget.PagedListView;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.CarUxRestrictionsHelper;
import com.android.car.settings.home.HomepageFragment;
import com.android.car.settings.users.UserIconProvider;
import com.android.car.settings.users.UserSwitcherFragment;

import java.util.concurrent.TimeUnit;

/**
 * Shows a page to access frequently used settings.
 */
public class QuickSettingFragment extends BaseFragment {
    private CarUserManagerHelper mCarUserManagerHelper;
    private UserIconProvider mUserIconProvider;
    private QuickSettingGridAdapter mGridAdapter;
    private PagedListView mListView;
    private View mFullSettingBtn;
    private View mUserSwitcherBtn;
    private HomeFragmentLauncher mHomeFragmentLauncher;
    private float mOpacityDisabled;
    private float mOpacityEnabled;

    /**
     * Returns an instance of this class.
     */
    public static QuickSettingFragment newInstance() {
        QuickSettingFragment quickSettingFragment = new QuickSettingFragment();
        Bundle bundle = QuickSettingFragment.getBundle();
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_quick_settings);
        bundle.putInt(EXTRA_LAYOUT, R.layout.quick_settings);
        bundle.putInt(EXTRA_TITLE_ID, R.string.settings_label);
        quickSettingFragment.setArguments(bundle);
        return quickSettingFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHomeFragmentLauncher = new HomeFragmentLauncher();
        Activity activity = requireActivity();
        activity.findViewById(R.id.action_bar_icon_container).setOnClickListener(
                v -> activity.finish());

        mOpacityDisabled = activity.getResources().getFloat(R.dimen.opacity_disabled);
        mOpacityEnabled = activity.getResources().getFloat(R.dimen.opacity_enabled);
        mCarUserManagerHelper = new CarUserManagerHelper(activity);
        mUserIconProvider = new UserIconProvider(mCarUserManagerHelper);
        mListView = activity.findViewById(R.id.list);
        mGridAdapter = new QuickSettingGridAdapter(activity);
        mListView.getRecyclerView().setLayoutManager(mGridAdapter.getGridLayoutManager());

        mFullSettingBtn = activity.findViewById(R.id.full_setting_btn);
        mFullSettingBtn.setOnClickListener(mHomeFragmentLauncher);
        mUserSwitcherBtn = activity.findViewById(R.id.user_switcher_btn);
        mUserSwitcherBtn.setOnClickListener(v -> {
            getFragmentController().launchFragment(UserSwitcherFragment.newInstance());
        });
        setupUserButton(activity);

        View exitBtn = activity.findViewById(R.id.exit_button);
        exitBtn.setOnClickListener(v -> getFragmentController().goBack());

        mGridAdapter
                .addTile(new WifiTile(activity, mGridAdapter, getFragmentController()))
                .addTile(new BluetoothTile(activity, mGridAdapter, getFragmentController()))
                .addTile(new DayNightTile(activity, mGridAdapter, getFragmentController()))
                .addTile(new CelluarTile(activity, mGridAdapter))
                .addSeekbarTile(new BrightnessTile(activity));
        mListView.setAdapter(mGridAdapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // In non-user builds (that is, user-debug, eng, etc), display some version information.
        if (!Build.IS_USER) {
            long buildTimeDiffDays =
                    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Build.TIME);
            String str = String.format(view.getResources().getString(R.string.build_info_fmt),
                    Build.FINGERPRINT, SystemProperties.get("ro.build.date", "<unknown>"),
                    buildTimeDiffDays);

            TextView buildInfo = view.requireViewById(R.id.build_info);
            buildInfo.setVisibility(View.VISIBLE);
            buildInfo.setText(str);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mGridAdapter.stop();
    }

    private void setupUserButton(Context context) {
        ImageView userIcon = requireActivity().findViewById(R.id.user_icon);
        UserInfo currentUserInfo = mCarUserManagerHelper.getCurrentForegroundUserInfo();
        userIcon.setImageDrawable(mUserIconProvider.getUserIcon(currentUserInfo, context));
        userIcon.clearColorFilter();

        TextView userSwitcherText = requireActivity().findViewById(R.id.user_switcher_text);
        userSwitcherText.setText(currentUserInfo.name);
    }

    /**
     * Quick setting fragment is distraction optimized, so is allowed at all times.
     */
    @Override
    public boolean canBeShown(@NonNull CarUxRestrictions carUxRestrictions) {
        return true;
    }

    @Override
    public void onUxRestrictionChanged(@NonNull CarUxRestrictions carUxRestrictions) {
        // TODO: update tiles
        applyRestriction(CarUxRestrictionsHelper.isNoSetup(carUxRestrictions));
    }

    private void applyRestriction(boolean restricted) {
        mHomeFragmentLauncher.showDOBlockingMessage(restricted);
        mFullSettingBtn.setAlpha(restricted ? mOpacityDisabled : mOpacityEnabled);
    }

    private class HomeFragmentLauncher implements OnClickListener {
        private boolean mShowDOBlockingMessage;

        private void showDOBlockingMessage(boolean show) {
            mShowDOBlockingMessage = show;
        }

        @Override
        public void onClick(View v) {
            if (mShowDOBlockingMessage) {
                getFragmentController().showDOBlockingMessage();
            } else {
                getFragmentController().launchFragment(HomepageFragment.newInstance());
            }
        }
    }
}
