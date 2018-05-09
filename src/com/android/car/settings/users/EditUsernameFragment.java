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

import android.annotation.IdRes;
import android.car.user.CarUserManagerHelper;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.TextInputEditText;
import android.view.View;
import android.widget.Button;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settingslib.util.SettingsConstants;

/**
 * Shows details for a user with the ability to edit the name, remove user and switch.
 */
public class EditUsernameFragment extends BaseFragment implements
        ConfirmRemoveUserDialog.ConfirmRemoveUserListener {
    public static final String EXTRA_USER_INFO = "extra_user_info";
    private static final String TAG = "EditUsernameFragment";
    private UserInfo mUserInfo;

    private TextInputEditText mUserNameEditText;
    private Button mOkButton;
    private Button mCancelButton;

    private CarUserManagerHelper mCarUserManagerHelper;

    public static EditUsernameFragment getInstance(UserInfo userInfo) {
        EditUsernameFragment
                userSettingsFragment = new EditUsernameFragment();
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
        mCarUserManagerHelper = new CarUserManagerHelper(getContext());

        configureUsernameEditing();

        showActionButtons();
    }

    @Override
    public void onRemoveUserConfirmed() {
        // If removing the current running foreground user, need to switch to another user
        // before deletion. Switch to a guest user for now, until default user logic is
        // implemented.
        if (mCarUserManagerHelper.removeUser(mUserInfo, "guest")) {
            getActivity().onBackPressed();
        }
    }

    private void configureUsernameEditing() {
        // Set the User's name.
        mUserNameEditText.setText(mUserInfo.name);

        // Configure OK button.
        mOkButton.setOnClickListener(view -> {
            // Save new user's name.
            mCarUserManagerHelper.setUserName(mUserInfo, mUserNameEditText.getText().toString());
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    SettingsConstants.USER_NAME_SET, 1);
            getActivity().onBackPressed();
        });

        // Configure Cancel button.
        mCancelButton.setOnClickListener(view -> {
            getActivity().onBackPressed();
        });

        // Each user can edit their own name.
        if (mCarUserManagerHelper.isForegroundUser(mUserInfo)) {
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

    private void showActionButtons() {
        if (mCarUserManagerHelper.isForegroundUser(mUserInfo)) {
            // Already in current user, shouldn't show SWITCH button.
            showRemoveUserButton(R.id.action_button1);
            return;
        }

        showRemoveUserButton(R.id.action_button2);
        showSwitchButton(R.id.action_button1);
    }

    private void showRemoveUserButton(@IdRes int buttonId) {
        Button removeUserBtn = (Button) getActivity().findViewById(buttonId);
        // If the current user is not allowed to remove users, the user trying to be removed
        // cannot be removed, or the current user is a demo user, do not show delete button.
        if (!mCarUserManagerHelper.canCurrentProcessRemoveUsers()
                || !mCarUserManagerHelper.canUserBeRemoved(mUserInfo)
                || mCarUserManagerHelper.isCurrentProcessDemoUser()) {
            removeUserBtn.setVisibility(View.GONE);
            return;
        }

        removeUserBtn.setVisibility(View.VISIBLE);
        removeUserBtn.setText(R.string.delete_button);
        removeUserBtn
                .setOnClickListener(v -> {
                    ConfirmRemoveUserDialog dialog =
                            new ConfirmRemoveUserDialog();
                    dialog.setConfirmRemoveUserListener(this);
                    dialog.show(this);
                });
    }

    private void showSwitchButton(@IdRes int buttonId) {
        Button switchUserBtn = (Button) getActivity().findViewById(buttonId);
        // If the current process is not allowed to switch to another user, doe not show the switch
        // button.
        if (!mCarUserManagerHelper.canCurrentProcessSwitchUsers()) {
            switchUserBtn.setVisibility(View.GONE);
            return;
        }
        switchUserBtn.setVisibility(View.VISIBLE);
        switchUserBtn.setText(R.string.user_switch);
        switchUserBtn.setOnClickListener(v -> {
            mCarUserManagerHelper.switchToUser(mUserInfo);
            getActivity().onBackPressed();
        });
    }
}
