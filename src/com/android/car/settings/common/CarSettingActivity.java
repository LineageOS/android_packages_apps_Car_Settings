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
package com.android.car.settings.common;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.InputMethodManager;

import com.android.car.settings.R;
import com.android.car.settings.home.HomepageFragment;
import com.android.car.settings.users.UsersListFragment;
import com.android.car.settings.wifi.WifiSettingsFragment;

/**
 * Base activity class for car settings, provides a action bar with a back button that goes to
 * previous activity.
 */
public class CarSettingActivity extends AppCompatActivity implements
        BaseFragment.FragmentController {
    private static final String TAG = "CarSetting";

    /** Actions to launch setting page to configure a new wifi network. */
    public static final String ACTION_ADD_WIFI = "android.car.settings.action_add_wifi";

    /** Actions to launch setting page to show the list of all users. */
    public static final String ACTION_LIST_USER = "android.car.settings.action_list_user";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_compat_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_LIST_USER:
                    launchFragment(UsersListFragment.newInstance());
                    return;
                case ACTION_ADD_WIFI:
                    launchFragment(WifiSettingsFragment.getInstance());
                    return;
                default:
            }
        }
        HomepageFragment homepageFragment = HomepageFragment.getInstance();
        homepageFragment.setFragmentController(this);
        launchFragment(homepageFragment);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void launchFragment(BaseFragment fragment) {
        fragment.setFragmentController(this);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.animator.trans_right_in ,
                        R.animator.trans_left_out,
                        R.animator.trans_left_in,
                        R.animator.trans_right_out)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goBack() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        hideKeyboard();
        // if the backstack is empty, finish the activity.
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)this.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }
}
