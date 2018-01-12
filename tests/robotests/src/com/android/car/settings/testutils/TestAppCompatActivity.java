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

package com.android.car.settings.testutils;

import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

/**
 * Test activity that extends {@link AppCompatActivity}.
 * Used for testing {@code BaseFragment} instances.
 */
public class TestAppCompatActivity extends AppCompatActivity {
    private UserManager mUserManager;
    private boolean mOnBackPressedFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_compat_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Places fragment in place of fragment container.
     *
     * @param fragment Fragment to add to activity.
     */
    public void createFragment(BaseFragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Convenient method to set a mock of a UserManager on activity.
     * The instance will get returned when {@code activity.getSystemService(Context.USER_SERVICE)}
     * is called.
     *
     * @param userManager Instance of UserManager to be set
     */
    public void setUserManager(UserManager userManager) {
        mUserManager = userManager;
    }

    /**
     * Override to catch onBackPressed invocations on the activity.
     */
    @Override
    public void onBackPressed() {
        mOnBackPressedFlag = true;
    }

    /**
     * Gets a boolean flag indicating whether onBackPressed has been called.
     *
     * @return {@code true} if onBackPressed called, {@code false} otherwise.
     */
    public boolean getOnBackPressedFlag() {
        return mOnBackPressedFlag;
    }

    /**
     * Clear the boolean flag for onBackPressed by setting it to false.
     */
    public void clearOnBackPressedFlag() {
        mOnBackPressedFlag = false;
    }

    @Override
    public Object getSystemService(String name) {
        if (name == Context.USER_SERVICE && mUserManager != null) {
            return mUserManager;
        }

        return getApplicationContext().getSystemService(name);
    }
}
