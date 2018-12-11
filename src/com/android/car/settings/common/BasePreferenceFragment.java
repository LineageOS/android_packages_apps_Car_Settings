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
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base fragment for all settings. Subclasses must provide a resource id via
 * {@link #getPreferenceScreenResId()} for the XML resource which defines the preferences to
 * display. This class is responsible for displaying the preferences and managing the associated
 * {@link BasePreferenceController} instances. When a preference is clicked, the controllers are
 * given a chance to handle the event before the default action.
 *
 * <p>Controllers are instantiated from preferences XML, combined with those created via
 * {@link #createPreferenceControllers(Context)}, and any controller implementing
 * {@link LifecycleObserver} is registered with this fragment's {@link Lifecycle}.
 *
 * <p>{@code preferenceTheme} must be specified in the application theme, and the parent to which
 * this fragment attaches must implement {@link UxRestrictionsProvider} and
 * {@link FragmentController} or an {@link IllegalStateException} will be thrown during
 * {@link #onAttach(Context)}. Changes to driving state restrictions are propagated to
 * controllers. If a controller becomes unavailable for a particular driving state, its
 * preference is hidden from the UI.
 *
 * @deprecated Use {@link SettingsFragment}.
 */
@Deprecated
public abstract class BasePreferenceFragment extends PreferenceFragmentCompat implements
        CarUxRestrictionsManager.OnUxRestrictionsChangedListener {

    private static final Logger LOG = new Logger(BasePreferenceFragment.class);

    private final Map<Class, List<BasePreferenceController>> mPreferenceControllersLookup =
            new ArrayMap<>();
    private final List<BasePreferenceController> mPreferenceControllers = new ArrayList<>();

    @Nullable
    private CarUxRestrictions mRestrictionInfo;

    /**
     * Returns the resource id for the preference XML of this fragment.
     */
    @XmlRes
    protected abstract int getPreferenceScreenResId();

    /**
     * Creates a list of {@link BasePreferenceController} instances for this fragment that are
     * not declared in the preference screen XML resource. Subclasses may override this method to
     * create controllers which cannot be declared in XML.
     */
    protected List<BasePreferenceController> createPreferenceControllers(Context context) {
        return Collections.emptyList();
    }

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
     * Returns the controller of the given {@param clazz} for the given {@param preferenceKeyResId}.
     * Subclasses may use this method in {@link #onAttach(Context)} to call setters on your
     * controller to pass arguments in addition to the context and preference key after
     * construction.
     *
     * <p>For example:
     * <pre>{@code
     * @Override
     * public void onAttach(Context context) {
     *     super.onAttach(context);
     *     use(MyPreferenceController.class, R.string.pk_my_key).setMyArg(myArg);
     * }
     * }</pre>
     */
    @SuppressWarnings("unchecked") // Class is used as map key.
    protected <T extends BasePreferenceController> T use(Class<T> clazz,
            @StringRes int preferenceKeyResId) {
        List<BasePreferenceController> controllerList = mPreferenceControllersLookup.get(clazz);
        if (controllerList != null) {
            String preferenceKey = getString(preferenceKeyResId);
            for (BasePreferenceController controller : controllerList) {
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
        /*
         * If this fragment was detached and then reattached, it is possible that the local
         * restriction info will match the restriction info from the provider, and the newly
         * constructed controllers will never be initialized. Setting null makes sure the local
         * value will not match the provider value so the controllers receive the restrictions
         * for the initial display.
         */
        mRestrictionInfo = null;

        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(androidx.preference.R.attr.preferenceTheme, tv,
                true);
        int theme = tv.resourceId;
        if (theme == 0) {
            throw new IllegalStateException("Must specify preferenceTheme in theme");
        }
        // Construct a context with the theme as controllers may create new preferences.
        Context styledContext = new ContextThemeWrapper(getActivity(), theme);

        mPreferenceControllers.clear();
        mPreferenceControllers.addAll(createPreferenceControllers(styledContext));
        mPreferenceControllers.addAll(
                PreferenceControllerListHelper.getPreferenceControllersFromXml(styledContext,
                        getPreferenceScreenResId(), (FragmentController) requireActivity()));

        Lifecycle lifecycle = getLifecycle();
        mPreferenceControllers.forEach(controller -> {
            if (controller instanceof LifecycleObserver) {
                lifecycle.addObserver(((LifecycleObserver) controller));
            }
            mPreferenceControllersLookup.computeIfAbsent(controller.getClass(),
                    k -> new ArrayList<>(/* initialCapacity= */ 1)).add(controller);
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        @XmlRes int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            return;
        }
        addPreferencesFromResource(resId);
        CarUxRestrictions restrictionInfo =
                ((UxRestrictionsProvider) requireActivity()).getCarUxRestrictions();
        updateUxRestrictions(restrictionInfo);
        // Allow controllers to set initial visibility.
        updateDisplayedPreferences();
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
    public void onStart() {
        super.onStart();
        CarUxRestrictions restrictionInfo =
                ((UxRestrictionsProvider) requireActivity()).getCarUxRestrictions();
        updateUxRestrictions(restrictionInfo);
        updateDisplayedPreferences();
        updatePreferenceStates();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Lifecycle lifecycle = getLifecycle();
        mPreferenceControllers.forEach(controller -> {
            if (controller instanceof LifecycleObserver) {
                lifecycle.removeObserver(((LifecycleObserver) controller));
            }
        });
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // Give all controllers a chance to handle click.
        for (BasePreferenceController controller : mPreferenceControllers) {
            if (controller.handlePreferenceTreeClick(preference)) {
                return true;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        updateUxRestrictions(restrictionInfo);
        updateDisplayedPreferences();
        updatePreferenceStates();
    }

    private void updateUxRestrictions(CarUxRestrictions restrictionInfo) {
        if (!restrictionInfo.isSameRestrictions(mRestrictionInfo)) {
            mRestrictionInfo = restrictionInfo;
            for (BasePreferenceController controller : mPreferenceControllers) {
                controller.onUxRestrictionsChanged(restrictionInfo);
            }
        }
    }

    /** Updates the visibility of preferences based on the current state. */
    private void updateDisplayedPreferences() {
        PreferenceScreen screen = getPreferenceScreen();
        for (BasePreferenceController controller : mPreferenceControllers) {
            controller.displayPreference(screen);
        }
    }

    /** Updates the contents of preferences based on the current state. */
    private void updatePreferenceStates() {
        PreferenceScreen screen = getPreferenceScreen();
        for (BasePreferenceController controller : mPreferenceControllers) {
            if (!controller.isAvailable()) {
                continue;
            }
            final String key = controller.getPreferenceKey();

            final Preference preference = screen.findPreference(key);
            if (preference == null) {
                LOG.d(String.format("Cannot find preference with key %s for controller %s",
                        key, controller.getClass().getSimpleName()));
                continue;
            }
            controller.updateState(preference);
        }
    }
}
