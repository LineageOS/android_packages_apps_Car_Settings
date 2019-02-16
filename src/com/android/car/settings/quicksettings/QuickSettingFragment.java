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
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
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
    // Time to delay refreshing the build info, if the clock is not correct.
    private static final long BUILD_INFO_REFRESH_TIME_MS = TimeUnit.SECONDS.toMillis(5);

    private CarUserManagerHelper mCarUserManagerHelper;
    private UserIconProvider mUserIconProvider;
    private QuickSettingGridAdapter mGridAdapter;
    private PagedListView mListView;
    private View mFullSettingsBtn;
    private View mUserSwitcherBtn;
    private HomeFragmentLauncher mHomeFragmentLauncher;
    private float mOpacityDisabled;
    private float mOpacityEnabled;
    private TextView mBuildInfo;

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_quick_settings;
    }

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.quick_settings;
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

        mFullSettingsBtn = activity.findViewById(R.id.full_settings_btn);
        mFullSettingsBtn.setOnClickListener(mHomeFragmentLauncher);
        mUserSwitcherBtn = activity.findViewById(R.id.user_switcher_btn);
        mUserSwitcherBtn.setOnClickListener(v -> {
            getFragmentController().launchFragment(new UserSwitcherFragment());
        });
        setupUserButton(activity);

        View exitBtn = activity.findViewById(R.id.action_bar_icon_container);
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

        mBuildInfo = view.requireViewById(R.id.build_info);
    }

    @Override
    public void onStart() {
        super.onStart();

        mUserSwitcherBtn.setVisibility(showUserSwitcher() ? View.VISIBLE : View.INVISIBLE);
        // In non-user builds (that is, user-debug, eng, etc), display some version information.
        if (!Build.IS_USER) {
            refreshBuildInfo();
        }
    }

    private void refreshBuildInfo() {
        if (!isAdded()) {
            // This can happen if the delayed post happens before we're stopped. Just give up
            // trying to get the right clock.
            return;
        }

        long buildTimeDiffDays =
                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Build.TIME);
        if (buildTimeDiffDays < 0) {
            // If it's in the past, that likely means the current time is wrong (or the build time
            // could be wrong, but that's less likely). Reschedule this to run in a few seconds to
            // see whether the clock's been fixed.
            mBuildInfo.postDelayed(this::refreshBuildInfo, BUILD_INFO_REFRESH_TIME_MS);
        }

        String str = String.format(getResources().getString(R.string.build_info_fmt),
                Build.FINGERPRINT, SystemProperties.get("ro.build.date", "<unknown>"),
                buildTimeDiffDays < 0 ? "--" : Long.toString(buildTimeDiffDays));

        mBuildInfo.setVisibility(View.VISIBLE);
        mBuildInfo.setText(str);
    }

    @Override
    public void onStop() {
        super.onStop();
        mGridAdapter.stop();
    }

    private void setupUserButton(Context context) {
        Button userButton = requireActivity().findViewById(R.id.user_switcher_btn);
        UserInfo currentUserInfo = mCarUserManagerHelper.getCurrentForegroundUserInfo();
        Drawable userIcon = mUserIconProvider.getUserIcon(currentUserInfo, context);
        userButton.setCompoundDrawablesRelativeWithIntrinsicBounds(userIcon, /* top= */
                null, /* end= */ null, /* bottom= */ null);
        userButton.setText(currentUserInfo.name);
    }

    private boolean showUserSwitcher() {
        return !UserManager.isDeviceInDemoMode(getContext())
            && UserManager.supportsMultipleUsers()
            && !UserManager.get(getContext()).hasUserRestriction(UserManager.DISALLOW_USER_SWITCH);
    }

    /**
     * Quick setting fragment is distraction optimized, so is allowed at all times.
     */
    @Override
    public boolean canBeShown(@NonNull CarUxRestrictions carUxRestrictions) {
        return true;
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        // TODO: update tiles
        applyRestriction(CarUxRestrictionsHelper.isNoSetup(restrictionInfo));
    }

    private void applyRestriction(boolean restricted) {
        mHomeFragmentLauncher.showBlockingMessage(restricted);
        mFullSettingsBtn.setAlpha(restricted ? mOpacityDisabled : mOpacityEnabled);
    }

    private class HomeFragmentLauncher implements OnClickListener {
        private boolean mShowBlockingMessage;

        private void showBlockingMessage(boolean show) {
            mShowBlockingMessage = show;
        }

        @Override
        public void onClick(View v) {
            if (mShowBlockingMessage) {
                getFragmentController().showBlockingMessage();
            } else {
                getFragmentController().launchFragment(new HomepageFragment());
            }
        }
    }
}
