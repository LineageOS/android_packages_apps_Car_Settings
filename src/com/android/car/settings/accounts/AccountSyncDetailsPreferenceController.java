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

package com.android.car.settings.accounts;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.Logger;
import com.android.car.settings.common.NoSetupPreferenceController;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller that presents all visible sync adapters for an account.
 *
 * <p>Largely derived from {@link com.android.settings.accounts.AccountSyncSettings}.
 */
public class AccountSyncDetailsPreferenceController extends NoSetupPreferenceController implements
        AuthenticatorHelper.OnAccountsUpdateListener, LifecycleObserver {
    private static final Logger LOG = new Logger(AccountSyncDetailsPreferenceController.class);
    /**
     * Preferences are keyed by authority so that existing SyncPreferences can be reused on account
     * sync.
     */
    private final Map<String, SyncPreference> mSyncPreferences = new ArrayMap<>();
    private final Set<SyncAdapterType> mInvisibleAdapters = new HashSet<>();
    private boolean mIsStopped = true;
    private Account mAccount;
    private UserHandle mUserHandle;
    private PreferenceGroup mSyncGroup;
    private AuthenticatorHelper mAuthenticatorHelper;
    private Object mStatusChangeListenerHandle;
    private SyncStatusObserver mSyncStatusObserver =
            which -> ThreadUtils.postOnMainThread(() -> forceUpdateSyncCategory());

    public AccountSyncDetailsPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    /** Sets the account that the sync preferences are being shown for. */
    public void setAccount(Account account) {
        mAccount = account;
    }

    /** Sets the user handle used by the controller. */
    public void setUserHandle(UserHandle userHandle) {
        mUserHandle = userHandle;
    }


    /**
     * Checks if the controller was initialized properly and initializes the authenticator helper.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        checkInitialized();
        mAuthenticatorHelper = new AuthenticatorHelper(mContext, mUserHandle, /* listener= */ this);
    }

    /**
     * Verifies that the controller was properly initialized with {@link #setAccount(Account)} and
     * {@link #setUserHandle(UserHandle)}.
     *
     * @throws IllegalStateException if the account or user handle is {@code null}
     */
    private void checkInitialized() {
        LOG.v("checkInitialized");
        if (mAccount == null) {
            throw new IllegalStateException(
                    "AccountSyncDetailsPreferenceController must be initialized by calling "
                            + "setAccount(Account)");
        }
        if (mUserHandle == null) {
            throw new IllegalStateException(
                    "AccountSyncDetailsPreferenceController must be initialized by calling "
                            + "setUserHandle(UserHandle)");
        }
    }

    /**
     * Registers the account update and sync status change callbacks.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mIsStopped = false;
        mAuthenticatorHelper.listenToAccountUpdates();

        mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
                        | ContentResolver.SYNC_OBSERVER_TYPE_STATUS
                        | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncStatusObserver);
    }

    /**
     * Unregisters the account update and sync status change callbacks.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mIsStopped = true;
        mAuthenticatorHelper.stopListeningToAccountUpdates();
        if (mStatusChangeListenerHandle != null) {
            ContentResolver.removeStatusChangeListener(mStatusChangeListenerHandle);
        }
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
        // Only force a refresh if accounts have changed for the current user.
        if (userHandle.equals(mUserHandle)) {
            forceUpdateSyncCategory();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        // Add preferences for each account if the controller should be available
        if (isAvailable()) {
            mSyncGroup = (PreferenceGroup) screen.findPreference(getPreferenceKey());
            forceUpdateSyncCategory();
        }
    }

    /** Forces a refresh of the sync adapter preferences. */
    private void forceUpdateSyncCategory() {
        // forceUpdateSyncCategory may be called after the fragment is stopped, so make sure that
        // it hasn't been
        if (!mIsStopped) {
            return;
        }
        Set<String> preferencesToRemove = new HashSet<>(mSyncPreferences.keySet());
        List<SyncPreference> preferences = getSyncPreferences(preferencesToRemove);

        // Sort the preferences, add the ones that need to be added, and remove the ones that need
        // to be removed. Manually set the order so that existing preferences are reordered
        // correctly.
        Collections.sort(preferences, Comparator.comparing(
                (SyncPreference a) -> a.getTitle().toString())
                .thenComparing((SyncPreference a) -> a.getSummary().toString()));

        for (int i = 0; i < preferences.size(); i++) {
            SyncPreference pref = preferences.get(i);
            pref.setOrder(i);
            mSyncPreferences.put(pref.getKey(), pref);
            mSyncGroup.addPreference(pref);
        }

        for (String key : preferencesToRemove) {
            mSyncGroup.removePreference(mSyncPreferences.get(key));
            mSyncPreferences.remove(key);
        }
    }

    /**
     * Returns a list of preferences corresponding to the visible sync adapters for the current
     * user.
     *
     * <p> Derived from {@link com.android.settings.accounts.AccountSyncSettings#setFeedsState}
     * and {@link com.android.settings.accounts.AccountSyncSettings#updateAccountSwitches}.
     *
     * @param preferencesToRemove the keys for the preferences currently being shown; only the keys
     *                            for preferences to be removed will remain after method execution
     */
    private List<SyncPreference> getSyncPreferences(Set<String> preferencesToRemove) {
        int userId = mUserHandle.getIdentifier();
        PackageManager packageManager = mContext.getPackageManager();
        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(
                mUserHandle.getIdentifier());
        List<SyncInfo> currentSyncs = ContentResolver.getCurrentSyncsAsUser(userId);
        // Whether one time sync is enabled rather than automtic sync
        boolean oneTimeSyncMode = !ContentResolver.getMasterSyncAutomaticallyAsUser(userId);
        boolean syncIsFailing = false;
        mInvisibleAdapters.clear();

        List<SyncPreference> syncPreferences = new ArrayList<>();

        for (int i = 0; i < syncAdapters.length; i++) {
            SyncAdapterType syncAdapter = syncAdapters[i];
            String authority = syncAdapter.authority;

            // If the sync adapter is not for this account type, don't show it
            if (!syncAdapter.accountType.equals(mAccount.type)) {
                continue;
            }

            if (!syncAdapter.isUserVisible()) {
                // keep track of invisible sync adapters, so sync now forces them to sync as well.
                // TODO: sync invisible adapters
                mInvisibleAdapters.add(syncAdapter);
                // If the sync adapter is not visible, don't show it
                continue;
            }

            boolean isSyncable = ContentResolver.getIsSyncableAsUser(mAccount, authority,
                    mUserHandle.getIdentifier()) > 0;
            // If the adapter is not syncable, don't show it
            if (!isSyncable) {
                continue;
            }

            int uid;
            try {
                uid = packageManager.getPackageUidAsUser(syncAdapter.getPackageName(), userId);
            } catch (PackageManager.NameNotFoundException e) {
                LOG.e("No uid for package" + syncAdapter.getPackageName(), e);
                // If we can't get the Uid for the package hosting the sync adapter, don't show it
                continue;
            }

            CharSequence title = getTitle(authority);
            if (TextUtils.isEmpty(title)) {
                continue;
            }

            // If we've reached this point, the sync adapter should be shown. If a preference for
            // the sync adapter already exists, update its state. Otherwise, create a new
            // preference.
            SyncPreference pref = mSyncPreferences.getOrDefault(authority,
                    new SyncPreference(mContext, authority));
            pref.setUid(uid);
            pref.setTitle(title);

            // Keep track of preferences that need to be added and removed
            syncPreferences.add(pref);
            preferencesToRemove.remove(authority);

            SyncStatusInfo status = ContentResolver.getSyncStatusAsUser(mAccount, authority,
                    userId);
            boolean syncEnabled = ContentResolver.getSyncAutomaticallyAsUser(mAccount, authority,
                    userId);
            boolean activelySyncing = isSyncing(currentSyncs, authority);

            // The preference should be checked if one one-time sync or regular sync is enabled
            boolean checked = oneTimeSyncMode || syncEnabled;
            pref.setChecked(checked);

            String summary = getSummary(status, syncEnabled, activelySyncing);
            pref.setSummary(summary);

            // Update the sync state so the icon is updated
            SyncPreference.SYNC_STATE syncState = getSyncState(status, syncEnabled,
                    activelySyncing);
            pref.setSyncState(syncState);
            pref.setOneTimeSyncMode(oneTimeSyncMode);

            // TODO: handle when sync is failing
            boolean syncIsPending = status != null && status.pending;
            if (syncState == SyncPreference.SYNC_STATE.FAILED && !activelySyncing
                    && !syncIsPending) {
                syncIsFailing = true;
            }
        }

        return syncPreferences;
    }

    /**
     * Returns the label for a given sync authority.
     *
     * @return the title if available, and an empty CharSequence otherwise
     */
    private CharSequence getTitle(String authority) {
        PackageManager packageManager = mContext.getPackageManager();
        ProviderInfo providerInfo = packageManager.resolveContentProviderAsUser(
                authority, /* flags= */ 0, mUserHandle.getIdentifier());
        if (providerInfo == null) {
            return "";
        }

        return providerInfo.loadLabel(packageManager);
    }

    private String getSummary(SyncStatusInfo status, boolean syncEnabled, boolean activelySyncing) {
        long successEndTime = (status == null) ? 0 : status.lastSuccessTime;
        // Set the summary based on the current syncing state
        if (!syncEnabled) {
            return mContext.getString(R.string.sync_disabled);
        } else if (activelySyncing) {
            return mContext.getString(R.string.sync_in_progress);
        } else if (successEndTime != 0) {
            Date date = new Date();
            date.setTime(successEndTime);
            String timeString = formatSyncDate(date);
            return mContext.getString(R.string.last_synced, timeString);
        }
        return "";
    }

    private SyncPreference.SYNC_STATE getSyncState(SyncStatusInfo status, boolean syncEnabled,
            boolean activelySyncing) {
        boolean initialSync = status != null && status.initialize;
        boolean syncIsPending = status != null && status.pending;
        boolean lastSyncFailed = syncEnabled && status != null && status.lastFailureTime != 0
                && status.getLastFailureMesgAsInt(0)
                != ContentResolver.SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS;
        if (activelySyncing && !initialSync) {
            return SyncPreference.SYNC_STATE.ACTIVE;
        } else if (syncIsPending && !initialSync) {
            return SyncPreference.SYNC_STATE.PENDING;
        } else if (lastSyncFailed) {
            return SyncPreference.SYNC_STATE.FAILED;
        }
        return SyncPreference.SYNC_STATE.NONE;
    }

    /** Returns whether a sync adapter is currently syncing for the account being shown. */
    private boolean isSyncing(List<SyncInfo> currentSyncs, String authority) {
        for (SyncInfo syncInfo : currentSyncs) {
            if (syncInfo.account.equals(mAccount) && syncInfo.authority.equals(authority)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    String formatSyncDate(Date date) {
        return DateFormat.getDateFormat(mContext).format(date) + " " + DateFormat.getTimeFormat(
                mContext).format(date);
    }

    /**
     * A preference that represents the state of a sync adapter.
     *
     * <p>Largely derived from {@link com.android.settings.accounts.SyncStateSwitchPreference}.
     */
    private static class SyncPreference extends SwitchPreference {
        // TODO: Use uid to launch permission check for syncing
        private int mUid;
        private SYNC_STATE mSyncState = SYNC_STATE.NONE;
        /**
         * A mode for this preference where clicking does a one-time sync instead of
         * toggling whether the provider will do autosync.
         */
        private boolean mOneTimeSyncMode = false;

        private SyncPreference(Context context, String authority) {
            super(context);
            setKey(authority);
            setPersistent(false);
            updateIcon();

            /* Disabled for now. TODO: Enable toggling */
            setEnabled(false);
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);

            // TODO: handle animation of syncing icon

            View switchView = view.findViewById(com.android.internal.R.id.switch_widget);
            if (mOneTimeSyncMode) {
                switchView.setVisibility(View.GONE);

                /*
                 * Override the summary. Fill in the %1$s with the existing summary
                 * (what ends up happening is the old summary is shown on the next
                 * line).
                 */
                TextView summary = (TextView) view.findViewById(android.R.id.summary);
                summary.setText(getContext().getString(R.string.sync_one_time_sync, getSummary()));
            } else {
                switchView.setVisibility(View.VISIBLE);
            }
        }

        public void setUid(int uid) {
            mUid = uid;
        }

        /** Updates the preference icon based on the current syncing state. */
        private void updateIcon() {
            switch (mSyncState) {
                case ACTIVE:
                case PENDING:
                    setIcon(R.drawable.ic_list_sync_anim);
                    break;
                case FAILED:
                    setIcon(R.drawable.ic_sync_problem);
                    break;
                default:
                    setIcon(null);
                    setIconSpaceReserved(true);
                    break;
            }
        }

        public void setSyncState(SYNC_STATE state) {
            mSyncState = state;
            updateIcon();
        }

        public void setOneTimeSyncMode(boolean oneTimeSyncMode) {
            mOneTimeSyncMode = oneTimeSyncMode;
            // Force a refresh so that onBindViewHolder is called
            notifyChanged();
        }

        private enum SYNC_STATE {
            ACTIVE,
            PENDING,
            FAILED,
            NONE
        }
    }
}
