/*
 * Copyright 2018 The Android Open Source Project
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

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base fragment for all settings. Subclasses must provide a resource id via
 * {@link #getPreferenceScreenResId()} for the XML resource which defines the preferences to
 * display and controllers to update their state. This class is responsible for displaying the
 * preferences, creating {@link PreferenceController} instances from the metadata, and
 * associating the preferences with their corresponding controllers.
 *
 * <p>{@code preferenceTheme} must be specified in the application theme, and the parent to which
 * this fragment attaches must implement {@link UxRestrictionsProvider} and
 * {@link FragmentController} or an {@link IllegalStateException} will be thrown during
 * {@link #onAttach(Context)}. Changes to driving state restrictions are propagated to
 * controllers.
 */
public abstract class SettingsFragment extends PreferenceFragmentCompat implements
        CarUxRestrictionsManager.OnUxRestrictionsChangedListener {

    private final Map<Class, List<PreferenceController>> mPreferenceControllersLookup =
            new ArrayMap<>();
    private final List<PreferenceController> mPreferenceControllers = new ArrayList<>();

    private CarUxRestrictions mUxRestrictions;

    /**
     * Returns the resource id for the preference XML of this fragment.
     */
    @XmlRes
    protected abstract int getPreferenceScreenResId();

    /**
     * Returns the {@link FragmentController}, this function should only be called after onAttach().
     */
    public final FragmentController getFragmentController() {
        return (FragmentController) requireActivity();
    }

    /**
     * Returns the layout id to use as the activity action bar. Subclasses should override this
     * method to customize the action bar layout (e.g. additional buttons, switches, etc.). The
     * default action bar contains a back button and the title.
     */
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar;
    }

    /**
     * Returns the controller of the given {@code clazz} for the given {@code
     * preferenceKeyResId}. Subclasses may use this method in {@link #onAttach(Context)} to call
     * setters on controllers to pass additional arguments after construction.
     *
     * <p>For example:
     * <pre>{@code
     * @Override
     * public void onAttach(Context context) {
     *     super.onAttach(context);
     *     use(MyPreferenceController.class, R.string.pk_my_key).setMyArg(myArg);
     * }
     * }</pre>
     *
     * <p>Important: Use judiciously to minimize tight coupling between controllers and fragments.
     */
    @SuppressWarnings("unchecked") // Class is used as map key.
    protected <T extends PreferenceController> T use(Class<T> clazz,
            @StringRes int preferenceKeyResId) {
        List<PreferenceController> controllerList = mPreferenceControllersLookup.get(clazz);
        if (controllerList != null) {
            String preferenceKey = getString(preferenceKeyResId);
            for (PreferenceController controller : controllerList) {
                if (controller.getPreferenceKey().equals(preferenceKey)) {
                    return (T) controller;
                }
            }
        }
        return null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof UxRestrictionsProvider)) {
            throw new IllegalStateException("Must attach to a UxRestrictionsProvider");
        }
        if (!(getActivity() instanceof FragmentController)) {
            throw new IllegalStateException("Must attach to a FragmentController");
        }

        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(androidx.preference.R.attr.preferenceTheme, tv,
                true);
        int theme = tv.resourceId;
        if (theme == 0) {
            throw new IllegalStateException("Must specify preferenceTheme in theme");
        }
        // Construct a context with the theme as controllers may create new preferences.
        Context styledContext = new ContextThemeWrapper(getActivity(), theme);

        mUxRestrictions = ((UxRestrictionsProvider) requireActivity()).getCarUxRestrictions();
        mPreferenceControllers.clear();
        mPreferenceControllers.addAll(
                PreferenceControllerListHelper2.getPreferenceControllersFromXml(styledContext,
                        getPreferenceScreenResId(), (FragmentController) requireActivity(),
                        mUxRestrictions));

        Lifecycle lifecycle = getLifecycle();
        mPreferenceControllers.forEach(controller -> {
            lifecycle.addObserver(controller);
            mPreferenceControllersLookup.computeIfAbsent(controller.getClass(),
                    k -> new ArrayList<>(/* initialCapacity= */ 1)).add(controller);
        });
    }

    /**
     * Inflates the preferences from {@link #getPreferenceScreenResId()} and associates the
     * preference with their corresponding {@link PreferenceController} instances.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        @XmlRes int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            throw new IllegalStateException(
                    "Fragment must specify a preference screen resource ID");
        }
        addPreferencesFromResource(resId);
        PreferenceScreen screen = getPreferenceScreen();
        for (PreferenceController controller : mPreferenceControllers) {
            controller.setPreference(screen.findPreference(controller.getPreferenceKey()));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FrameLayout actionBarContainer = requireActivity().findViewById(R.id.action_bar);
        if (actionBarContainer != null) {
            actionBarContainer.removeAllViews();
            getLayoutInflater().inflate(getActionBarLayoutId(), actionBarContainer);

            TextView titleView = actionBarContainer.requireViewById(R.id.title);
            titleView.setText(getPreferenceScreen().getTitle());
            actionBarContainer.requireViewById(R.id.action_bar_icon_container).setOnClickListener(
                    v -> requireActivity().onBackPressed());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Lifecycle lifecycle = getLifecycle();
        mPreferenceControllers.forEach(lifecycle::removeObserver);
    }

    /**
     * Notifies {@link PreferenceController} instances of changes to {@link CarUxRestrictions}.
     */
    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions uxRestrictions) {
        if (!uxRestrictions.isSameRestrictions(mUxRestrictions)) {
            mUxRestrictions = uxRestrictions;
            for (PreferenceController controller : mPreferenceControllers) {
                controller.onUxRestrictionsChanged(uxRestrictions);
            }
        }
    }
}
