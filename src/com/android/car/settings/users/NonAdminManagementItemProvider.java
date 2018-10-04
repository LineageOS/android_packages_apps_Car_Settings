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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.CompoundButton;

import androidx.car.widget.ActionListItem;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;

/**
 * Provides list items for screen that manages non-admin permissions.
 */
public class NonAdminManagementItemProvider extends AbstractRefreshableListItemProvider {
    private final UserRestrictionsListener mUserRestrictionsListener;
    private final UserRestrictionsProvider mUserRestrictionsProvider;
    private final Drawable mUserIcon;

    /**
     * An {@link AbstractRefreshableListItemProvider} that provides an admin with items that manage
     * the permissions/restrictions of another, non-admin, user.
     *
     * @param userIcon System icon for the user whose permissions are being managed
     */
    NonAdminManagementItemProvider(Context context,
            UserRestrictionsListener userRestrictionsListener,
            UserRestrictionsProvider userRestrictionsProvider,
            Drawable userIcon) {
        super(context);
        mUserRestrictionsListener = userRestrictionsListener;
        mUserRestrictionsProvider = userRestrictionsProvider;
        mUserIcon = userIcon;
        populateItems();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds items for managing the permissions/restrictions of a non-admin.
     */
    @Override
    protected void populateItems() {
        mItems.add(createGrantAdminItem());
        mItems.add(createCreateUserItem());
        mItems.add(createOutgoingCallsItem());
        mItems.add(createSmsMessagingItem());
    }

    private ActionListItem createGrantAdminItem() {
        ActionListItem grantAdminItem = new ActionListItem(mContext);
        grantAdminItem.setPrimaryActionIcon(mUserIcon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        grantAdminItem.setTitle(mContext.getString(R.string.grant_admin_permissions_title));
        grantAdminItem.setAction(mContext.getString(R.string.grant_admin_permissions_button_text),
                /* showDivider= */ false,
                v -> mUserRestrictionsListener.onGrantAdminPermission());

        return grantAdminItem;
    }

    private TextListItem createCreateUserItem() {
        boolean canCreateUsers = mUserRestrictionsProvider.hasCreateUserPermission();

        TextListItem createUserItem = new TextListItem(mContext);
        createUserItem.setTitle(mContext.getText(R.string.create_user_permission_title));
        createUserItem.setBody(mContext.getText(R.string.create_user_permission_body));
        createUserItem.setSwitch(canCreateUsers, /* showDivider= */ false,
                (CompoundButton buttonView, boolean checked) ->
                        mUserRestrictionsListener.onCreateUserPermissionChanged(checked));

        return createUserItem;
    }

    private TextListItem createOutgoingCallsItem() {
        boolean canMakeOutgoingCalls = mUserRestrictionsProvider.hasOutgoingCallsPermission();

        TextListItem outgoingCallsItem = new TextListItem(mContext);
        outgoingCallsItem.setTitle(mContext.getText(R.string.outgoing_calls_permission_title));
        outgoingCallsItem.setBody(mContext.getText(R.string.outgoing_calls_permission_body));
        outgoingCallsItem.setSwitch(canMakeOutgoingCalls, /* showDivider= */ false,
                (CompoundButton buttonView, boolean checked) ->
                        mUserRestrictionsListener.onOutgoingCallsPermissionChanged(checked));

        return outgoingCallsItem;
    }

    private TextListItem createSmsMessagingItem() {
        boolean canSendAndReceiveMessaging = mUserRestrictionsProvider.hasSmsMessagingPermission();

        TextListItem messagingItem = new TextListItem(mContext);
        messagingItem.setTitle(mContext.getText(R.string.sms_messaging_permission_title));
        messagingItem.setBody(mContext.getText(R.string.sms_messaging_permission_body));
        messagingItem.setSwitch(canSendAndReceiveMessaging, /* showDivider= */ false,
                (CompoundButton buttonView, boolean checked) ->
                        mUserRestrictionsListener.onSmsMessagingPermissionChanged(checked));

        return messagingItem;
    }

    /**
     * Interface for registering changes to user permissions.
     */
    interface UserRestrictionsListener {
        /**
         * Called when admin permissions should be granted.
         */
        void onGrantAdminPermission();

        /**
         * Called when the create user permission should be changed.
         */
        void onCreateUserPermissionChanged(boolean granted);

        /**
         * Called when the outgoing calls permission should be changed.
         */
        void onOutgoingCallsPermissionChanged(boolean granted);

        /**
         * Called when the sms messaging permission should be changed.
         */
        void onSmsMessagingPermissionChanged(boolean granted);
    }

    /**
     * Interface for providing the current state of User Restrictions.
     */
    interface UserRestrictionsProvider {
        /** Returns whether the user can create other users. */
        boolean hasCreateUserPermission();

        /** Returns whether the user has the Outgoing Calls permission. */
        boolean hasOutgoingCallsPermission();

        /** Returns whether the user has the SMS Messaging permission. */
        boolean hasSmsMessagingPermission();
    }
}

