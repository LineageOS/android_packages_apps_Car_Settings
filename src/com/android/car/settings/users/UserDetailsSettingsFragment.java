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
package com.android.car.settings.users;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.design.widget.TextInputEditText;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

/**
 * Shows details for a user with the ability to edit the name, remove user and switch.
 */
public class UserDetailsSettingsFragment extends BaseFragment {
    public static final String EXTRA_USER_INFO = "extra_user_info";
    private static final String TAG = "UserDetailsSettingsFragment";
    private UserInfo mUserInfo;
    private UserManager mUserManager;

    private boolean mCurrentUserIsOwner;
    private boolean mIsCurrentUser;

    private TextInputEditText mUserNameEditText;
    private Button mOkButton;
    private Button mCancelButton;

    public static UserDetailsSettingsFragment getInstance(UserInfo userInfo) {
        UserDetailsSettingsFragment
                userSettingsFragment = new UserDetailsSettingsFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_TITLE_ID, R.string.user_settings_details_title);
        bundle.putParcelable(EXTRA_USER_INFO, userInfo);
        bundle.putInt(EXTRA_LAYOUT, R.layout.user_details_fragment);
        userSettingsFragment.setArguments(bundle);
        return userSettingsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserInfo = getArguments().getParcelable(EXTRA_USER_INFO);
        mCurrentUserIsOwner = ActivityManager.getCurrentUser() == UserHandle.USER_SYSTEM;
        mIsCurrentUser = ActivityManager.getCurrentUser() == mUserInfo.id;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mUserNameEditText = (TextInputEditText) view.findViewById(R.id.user_name_text_edit);
        mOkButton = (Button) view.findViewById(R.id.ok_button);
        mCancelButton = (Button) view.findViewById(R.id.cancel_button);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);

        configureUsernameEditing();

        showRemoveUserButton();
        showSwitchButton();
    }

    private void configureUsernameEditing() {
        // Set the User's name.
        mUserNameEditText.setText(mUserInfo.name);

        // Configure OK button.
        mOkButton.setOnClickListener(view -> {
            // Save new user's name.
            mUserManager.setUserName(mUserInfo.id, mUserNameEditText.getText().toString());
            getActivity().onBackPressed();
        });

        // Configure Cancel button.
        mCancelButton.setOnClickListener(view -> {
            getActivity().onBackPressed();
        });

        if (mIsCurrentUser /* Each user can edit their own name. */
                || mCurrentUserIsOwner /* Owner can edit everyone's name. */) {
            allowUserNameEditing();
        } else {
            mUserNameEditText.setEnabled(false);
        }
    }

    private void allowUserNameEditing() {
        mUserNameEditText.setEnabled(true);
        mUserNameEditText.setSelectAllOnFocus(true);
        mUserNameEditText.setOnFocusChangeListener((view, focus) -> {
            if (focus || !mUserNameEditText.getText().toString().equals(mUserInfo.name)) {
                // If name editor is in focus, or the user's name is changed, show OK and Cancel
                // buttons to confirm or cancel the change.
                mOkButton.setVisibility(View.VISIBLE);
                mCancelButton.setVisibility(View.VISIBLE);
            } else {
                // Hide the buttons when user is not changing the user name.
                mOkButton.setVisibility(View.GONE);
                mCancelButton.setVisibility(View.GONE);
            }
        });
    }

    private void showRemoveUserButton() {
        TextView removeUserBtn = (TextView) getActivity().findViewById(R.id.action_button1);
        removeUserBtn.setText(R.string.delete_button);
        removeUserBtn.setOnClickListener(v -> removeUser());
    }

    private void showSwitchButton() {
        if (!mIsCurrentUser) {
            TextView switchUserBtn = (TextView) getActivity().findViewById(R.id.action_button2);
            switchUserBtn.setVisibility(View.VISIBLE);
            switchUserBtn.setText(R.string.user_switch);
            switchUserBtn.setOnClickListener(v -> switchTo());
        }
    }

    private void removeUser() {
        if (mUserInfo.id == UserHandle.USER_SYSTEM) {
            Log.w(TAG, "User " + mUserInfo.id + " could not removed.");
            return;
        }
        if (mIsCurrentUser) {
            switchToUserId(UserHandle.USER_SYSTEM);
        }
        if (mUserManager.removeUser(mUserInfo.id)) {
            getActivity().onBackPressed();
        }
    }

    private void switchTo() {
        if (mIsCurrentUser) {
            if (mUserInfo.isGuest()) {
                switchToGuest();
            }
            return;
        }

        if (UserManager.isGuestUserEphemeral()) {
            // If switching from guest, we want to bring up the guest exit dialog instead of
            // switching
            UserInfo currUserInfo = mUserManager.getUserInfo(ActivityManager.getCurrentUser());
            if (currUserInfo != null && currUserInfo.isGuest()) {
                return;
            }
        }

        switchToUserId(mUserInfo.id);
        getActivity().onBackPressed();
    }

    private void switchToGuest() {
        UserInfo guest = mUserManager.createGuest(getContext(),
                getContext().getString(R.string.user_guest));
        if (guest == null) {
            // Couldn't create user, most likely because there are too many, but we haven't
            // been able to reload the list yet.
            Log.w(TAG, "can't create user.");
            return;
        }
        switchToUserId(guest.id);
    }

    private void switchToUserId(int id) {
        try {
            ActivityManager.getService().switchUser(id);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't switch user.", e);
        }
    }
}
