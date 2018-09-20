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

package com.android.car.settings.users;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.os.UserManager;
import android.view.View;
import android.widget.CompoundButton;

import androidx.car.widget.ActionListItem;
import androidx.car.widget.TextListItem;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.R;
import com.android.car.settings.testutils.ShadowActionListItem;
import com.android.car.settings.testutils.ShadowTextListItem;
import com.android.car.settings.testutils.ShadowUserIconProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

/**
 * Tests for UsersItemProviderTest.
 */
@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowActionListItem.class, ShadowTextListItem.class,
        ShadowUserIconProvider.class})
public class NonAdminManagementItemProviderTest {
    private static final int NUM_ITEMS = 4;

    @Mock
    private NonAdminManagementItemProvider.UserRestrictionsListener mListener;
    @Mock
    private NonAdminManagementItemProvider.UserRestrictionsProvider mRestrictionsProvider;
    @Mock
    private UserManager mUserManager;

    private NonAdminManagementItemProvider mProvider;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);

        ShadowApplication shadowApp = ShadowApplication.getInstance();
        shadowApp.setSystemService(Context.USER_SERVICE, mUserManager);

        mProvider = createProvider();
    }

    @Test
    public void testItems_shouldHaveCorrectNumberOfItems() {
        assertThat(mProvider.size()).isEqualTo(NUM_ITEMS);
    }

    @Test
    public void testItems_firstItemShouldHaveGrantAdminTitle() {
        ActionListItem grandAdminItem = (ActionListItem) mProvider.get(0);

        assertThat(((ShadowActionListItem) Shadow.extract(
                grandAdminItem)).getTitle().toString()).isEqualTo(
                application.getString(R.string.grant_admin_permissions_title));
    }

    @Test
    public void testItems_firstItemShouldHaveGrantAdminBody() {
        ActionListItem grandAdminItem = (ActionListItem) mProvider.get(0);

        assertThat(((ShadowActionListItem) Shadow.extract(
                grandAdminItem)).getPrimaryActionText().toString()).isEqualTo(
                application.getString(R.string.grant_admin_permissions_button_text));
    }

    @Test
    public void testItems_firstItemActionClicked_shouldCallOnGrantAdminPermission() {
        ActionListItem grandAdminItem = (ActionListItem) mProvider.get(0);

        View.OnClickListener listener = ((ShadowActionListItem) Shadow.extract(
                grandAdminItem)).getPrimaryActionOnClickListener();
        listener.onClick(null);

        verify(mListener).onGrantAdminPermission();
    }

    @Test
    public void testItems_secondItemShouldHaveCreateUserPermissionTitle() {
        TextListItem createUserItem = (TextListItem) mProvider.get(1);

        assertThat(((ShadowTextListItem) Shadow.extract(
                createUserItem)).getTitle().toString()).isEqualTo(
                application.getText(R.string.create_user_permission_title));
    }

    @Test
    public void testItems_secondItemShouldHaveCreateUserPermissionBody() {
        TextListItem createUserItem = (TextListItem) mProvider.get(1);

        assertThat(((ShadowTextListItem) Shadow.extract(
                createUserItem)).getBody().toString()).isEqualTo(
                application.getText(R.string.create_user_permission_body));
    }

    @Test
    public void testItems_hasCreateUserPermission_secondItemChecked() {
        when(mRestrictionsProvider.hasCreateUserPermission()).thenReturn(true);
        // Recreate the provider so the mocked changes take effect.
        mProvider = createProvider();
        TextListItem createUserItem = (TextListItem) mProvider.get(1);

        assertThat(((ShadowTextListItem) Shadow.extract(
                createUserItem)).getSwitchChecked()).isTrue();
    }

    @Test
    public void testItems_doesNotHaveCreateUserPermission_secondItemNotChecked() {
        when(mRestrictionsProvider.hasCreateUserPermission()).thenReturn(false);
        // Recreate the provider so the mocked changes take effect.
        mProvider = createProvider();
        TextListItem createUserItem = (TextListItem) mProvider.get(1);

        assertThat(((ShadowTextListItem) Shadow.extract(
                createUserItem)).getSwitchChecked()).isFalse();
    }

    @Test
    public void testItems_secondItemOnCheckedChanged_shouldCallOnCreateUserPermissionChanged() {
        TextListItem createUserItem = (TextListItem) mProvider.get(1);

        CompoundButton.OnCheckedChangeListener listener = ((ShadowTextListItem) Shadow.extract(
                createUserItem)).getSwitchOnCheckedChangeListener();
        listener.onCheckedChanged(null, true);

        verify(mListener).onCreateUserPermissionChanged(true);
    }

    @Test
    public void testItems_thirdItemShouldHaveOutgoingCallsPermissionTitle() {
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(2);

        assertThat(((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getTitle().toString()).isEqualTo(
                application.getText(R.string.outgoing_calls_permission_title));
    }

    @Test
    public void testItems_thirdItemShouldHaveOutgoingCallsPermissionBody() {
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(2);

        assertThat(((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getBody().toString()).isEqualTo(
                application.getText(R.string.outgoing_calls_permission_body));
    }

    @Test
    public void testItems_hasOutgoingCallsPermission_thirdItemChecked() {
        when(mRestrictionsProvider.hasOutgoingCallsPermission()).thenReturn(true);
        // Recreate the provider so the mocked changes take effect.
        mProvider = createProvider();
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(2);

        assertThat(((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getSwitchChecked()).isTrue();
    }

    @Test
    public void testItems_doesNotHaveOutgoingCallsPermission_thirdItemNotChecked() {
        when(mRestrictionsProvider.hasOutgoingCallsPermission()).thenReturn(false);
        // Recreate the provider so the mocked changes take effect.
        mProvider = createProvider();
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(2);

        assertThat(((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getSwitchChecked()).isFalse();
    }

    @Test
    public void testItems_thirdItemOnCheckedChanged_shouldCallOnOutgoingCallsPermissionChanged() {
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(2);

        CompoundButton.OnCheckedChangeListener listener = ((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getSwitchOnCheckedChangeListener();
        listener.onCheckedChanged(null, true);

        verify(mListener).onOutgoingCallsPermissionChanged(true);
    }

    @Test
    public void testItems_fourthItemShouldHaveMessagingPermissionTitle() {
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(3);

        assertThat(((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getTitle().toString()).isEqualTo(
                application.getText(R.string.sms_messaging_permission_title));
    }

    @Test
    public void testItems_fourthItemShouldHaveMessagingPermissionBody() {
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(3);

        assertThat(((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getBody().toString()).isEqualTo(
                application.getText(R.string.sms_messaging_permission_body));
    }

    @Test
    public void testItems_hasMessagingPermission_fourthItemChecked() {
        when(mRestrictionsProvider.hasSmsMessagingPermission()).thenReturn(true);
        // Recreate the provider so the mocked changes take effect.
        mProvider = createProvider();
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(3);

        assertThat(((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getSwitchChecked()).isTrue();
    }

    @Test
    public void testItems_doesNotHaveMessagingPermission_fourthItemNotChecked() {
        when(mRestrictionsProvider.hasSmsMessagingPermission()).thenReturn(false);
        // Recreate the provider so the mocked changes take effect.
        mProvider = createProvider();
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(3);

        assertThat(((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getSwitchChecked()).isFalse();
    }

    @Test
    public void testItems_fourthItemOnCheckedChanged_shouldCallOnMessagingPermissionChanged() {
        TextListItem outgoingCallsItem = (TextListItem) mProvider.get(3);

        CompoundButton.OnCheckedChangeListener listener = ((ShadowTextListItem) Shadow.extract(
                outgoingCallsItem)).getSwitchOnCheckedChangeListener();
        listener.onCheckedChanged(null, true);

        verify(mListener).onSmsMessagingPermissionChanged(true);
    }

    private NonAdminManagementItemProvider createProvider() {
        return new NonAdminManagementItemProvider(application, mListener, mRestrictionsProvider,
                null);
    }
}
