/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.settings.common;

import static com.android.car.settings.common.ExtraSettingsLoader.META_DATA_IS_TOP_LEVEL_EXTRA_SETTINGS;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;
import com.android.car.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.car.settings.common.CarSettingActivities.HomepageActivity;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.Collections;
import java.util.List;

/**
 * Top level settings menu.
 */
public class TopLevelMenuFragment extends SettingsFragment {

    /**
     * The preference key for the top-level menu item associated with a fragment.
     * This is intended to be included with fragments launched from top-level menu
     * preferences using the {@link #launchFragment} method.
     */
    public static final String FRAGMENT_MENU_PREFERENCE_KEY = "fragment_menu_preference_key";

    private static final String KEY_SAVED_SELECTED_PREFERENCE_KEY = "saved_selected_preference_key";

    private MenuItem mSearchButton;
    private String mSelectedPreferenceKey;

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.homepage_fragment;
    }

    @Override
    protected List<MenuItem> getToolbarMenuItems() {
        return Collections.singletonList(mSearchButton);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchButton = new MenuItem.Builder(getContext())
                .setToSearch()
                .setOnClickListener(i -> onSearchButtonClicked())
                .setUxRestrictions(CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD)
                .build();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceHighlight(getHeaderKeyFromActivity());
    }

    @Override
    protected void setupToolbar(@NonNull ToolbarController toolbar) {
        super.setupToolbar(toolbar);
        toolbar.setTitle(R.string.settings_label);
        if (ActivityEmbeddingUtils.isEmbeddingSplitActivated(getActivity())) {
            toolbar.setLogo(R.drawable.ic_launcher_settings);
            toolbar.setNavButtonMode(NavButtonMode.DISABLED);
        } else {
            toolbar.setLogo(null);
            toolbar.setNavButtonMode(NavButtonMode.BACK);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getToolbar() != null) {
            setupToolbar(getToolbar());
        }
        if (savedInstanceState != null
                && savedInstanceState.getString(KEY_SAVED_SELECTED_PREFERENCE_KEY) != null) {
            updatePreferenceHighlight(
                    savedInstanceState.getString(KEY_SAVED_SELECTED_PREFERENCE_KEY));
        } else {
            if (mSelectedPreferenceKey != null) {
                updatePreferenceHighlight(mSelectedPreferenceKey);
            } else {
                updatePreferenceHighlight(getHeaderKeyFromActivity());
            }
        }
    }

    @Nullable
    private String getHeaderKeyFromActivity() {
        if (getActivity() == null || !(getActivity() instanceof HomepageActivity)) {
            return null;
        }
        return ((HomepageActivity) getActivity()).getTopLevelHeaderKey();
    }

    private void setSelectedPreferenceKey(@Nullable String key) {
        mSelectedPreferenceKey = key;
        if (getActivity() != null && getActivity() instanceof HomepageActivity) {
            ((HomepageActivity) getActivity()).setTopLevelHeaderKey(key);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        setSelectedPreferenceKey(mSelectedPreferenceKey);
        outState.putString(KEY_SAVED_SELECTED_PREFERENCE_KEY, mSelectedPreferenceKey);
    }

    @Override
    public CarUiRecyclerView onCreateCarUiRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        inflater.inflate(R.layout.top_level_recyclerview, parent, /* attachToRoot= */ true);
        return parent.findViewById(R.id.top_level_recycler_view);
    }

    @Override
    public void launchFragment(@Nullable Fragment fragment) {
        if (fragment == null) {
            return;
        }
        String preferenceKey = null;
        if (fragment.getArguments() != null) {
            preferenceKey = fragment.getArguments().getString(FRAGMENT_MENU_PREFERENCE_KEY);
        }
        updatePreferenceHighlight(preferenceKey);
        super.launchFragment(fragment);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getFragment() == null
                && !preference.getExtras().getBoolean(META_DATA_IS_TOP_LEVEL_EXTRA_SETTINGS)) {
            // Launching a new activity that is not a top level injected settings
            return super.onPreferenceTreeClick(preference);
        }
        updatePreferenceHighlight(preference.getKey());
        return super.onPreferenceTreeClick(preference);
    }
    @Override
    protected HighlightablePreferenceGroupAdapter createHighlightableAdapter(
            PreferenceScreen preferenceScreen) {
        return new HighlightablePreferenceGroupAdapter(preferenceScreen,
                R.drawable.top_level_preference_background,
                R.drawable.top_level_preference_highlight);
    }

    private void updatePreferenceHighlight(String key) {
        setSelectedPreferenceKey(key);
        if (!TextUtils.isEmpty(mSelectedPreferenceKey)) {
            requestPreferenceHighlight(mSelectedPreferenceKey);
        } else {
            clearPreferenceHighlight();
        }
    }

    @VisibleForTesting
    String getSelectedPreferenceKey() {
        return mSelectedPreferenceKey;
    }

    private void onSearchButtonClicked() {
        Intent intent = new Intent(Settings.ACTION_APP_SEARCH_SETTINGS)
                .setPackage(getSettingsIntelligencePkgName(getContext()));
        if (intent.resolveActivity(getContext().getPackageManager()) == null) {
            return;
        }
        startActivity(intent);
    }

    private String getSettingsIntelligencePkgName(Context context) {
        return context.getString(R.string.config_settingsintelligence_package_name);
    }
}
