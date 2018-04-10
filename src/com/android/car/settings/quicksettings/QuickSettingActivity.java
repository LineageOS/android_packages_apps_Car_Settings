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
 * limitations under the License
 */
package com.android.car.settings.quicksettings;

import android.app.Dialog;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.car.app.CarAlertDialog;
import androidx.car.widget.PagedListView;

import com.android.car.settings.R;
import com.android.car.settings.common.CarSettingActivity;
import com.android.car.settings.common.CarUxRestrictionsHelper;
import com.android.car.settings.users.UserIconProvider;
import com.android.settingslib.users.UserManagerHelper;

/**
 * Shows a page to access frequently used settings.
 */
public class QuickSettingActivity extends AppCompatActivity {
    private static final String TAG = "QS";
    private static final String DIALOG_TAG = "block_dialog_tag";

    private static final float RESTRICTED_ALPHA = 0.5f;
    private static final float UNRESTRICTED_ALPHA = 1f;

    private CarUxRestrictionsHelper mUxRestrictionsHelper;
    private UserManagerHelper  mUserManagerHelper;
    private QuickSettingGridAdapter mGridAdapter;
    private PagedListView mListView;
    private View mFullSettingBtn;
    private View mUserSwitcherBtn;

    private final OnClickListener mLaunchSettingListener = v -> {
        Intent intent = new Intent(this, CarSettingActivity.class);
        startActivity(intent);
    };

    private final OnClickListener mBlockingListener = v -> {
        AlertDialogFragment alertDialog = new AlertDialogFragment();
        alertDialog.show(getSupportFragmentManager(), DIALOG_TAG);
    };

    /**
     * Shows a dialog to notify user that the actions is not available while driving.
     */
    public static class AlertDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getContext();
            return new CarAlertDialog.Builder(context)
                    .setBody(context.getString(R.string.restricted_while_driving))
                    .setPositiveButton(context.getString(R.string.okay),
                            /* listener= */ null)
                    .create();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserManagerHelper = new UserManagerHelper(this);
        setContentView(R.layout.quick_settings);
        mListView = (PagedListView) findViewById(R.id.list);
        mGridAdapter = new QuickSettingGridAdapter(this);
        mListView.getRecyclerView().setLayoutManager(mGridAdapter.getGridLayoutManager());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // make the toolbar take the whole width.
        toolbar.setPadding(0, 0, 0, 0);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setCustomView(R.layout.action_bar_quick_settings);
        actionBar.setDisplayShowCustomEnabled(true);

        mFullSettingBtn = findViewById(R.id.full_setting_btn);
        mUserSwitcherBtn = findViewById(R.id.user_switcher_btn);
        mUserSwitcherBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CarSettingActivity.class);
            intent.setAction(CarSettingActivity.ACTION_LIST_USER);
            startActivity(intent);
        });

        setupAccountButton();
        View exitBtn = findViewById(R.id.exit_button);
        exitBtn.setOnClickListener(v -> finish());
        mUxRestrictionsHelper =
                new CarUxRestrictionsHelper(
                        this, QuickSettingActivity.this::onUxRestrictionChagned);
    }

    private void setupAccountButton() {
        ImageView userIcon = (ImageView) findViewById(R.id.user_icon);
        UserInfo currentUserInfo = mUserManagerHelper.getForegroundUserInfo();
        userIcon.setImageDrawable(
                UserIconProvider.getUserIcon(currentUserInfo, mUserManagerHelper, this));

        TextView userSwitcherText = (TextView) findViewById(R.id.user_switcher_text);
        userSwitcherText.setText(currentUserInfo.name);
    }

    @Override
    public void onStart() {
        super.onStart();

        mGridAdapter
                .addTile(new WifiTile(this, mGridAdapter))
                .addTile(new BluetoothTile(this, mGridAdapter))
                .addTile(new DayNightTile(this, mGridAdapter))
                .addSeekbarTile(new BrightnessTile(this));
        mListView.setAdapter(mGridAdapter);
        mUxRestrictionsHelper.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGridAdapter.stop();
        mUxRestrictionsHelper.stop();
    }

    private void onUxRestrictionChagned(CarUxRestrictions carUxRestrictions) {
        // TODO: update tiles
        applyRestriction(
                (carUxRestrictions.getActiveRestrictions()
                        & CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP)
                        == CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP);
    }

    private void applyRestriction(boolean restricted) {
        if (restricted) {
            mFullSettingBtn.setAlpha(RESTRICTED_ALPHA);
            mFullSettingBtn.setOnClickListener(mBlockingListener);
        } else {
            mFullSettingBtn.setAlpha(UNRESTRICTED_ALPHA);
            mFullSettingBtn.setOnClickListener(mLaunchSettingListener);
        }
    }
}
