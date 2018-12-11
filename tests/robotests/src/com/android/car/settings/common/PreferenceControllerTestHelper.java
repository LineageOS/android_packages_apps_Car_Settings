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

import static org.mockito.Mockito.mock;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Helper for testing {@link PreferenceController} classes.
 *
 * @param <T> the type of preference controller under test.
 */
public class PreferenceControllerTestHelper<T extends PreferenceController> {

    private static final String PREFERENCE_KEY = "preference_key";
    private static final CarUxRestrictions UX_RESTRICTIONS =
            new CarUxRestrictions.Builder(/* reqOpt= */ true,
                    CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(
            new LifecycleOwner() {
                @NonNull
                @Override
                public Lifecycle getLifecycle() {
                    return mLifecycleRegistry;
                }
            });
    private final FragmentController mMockFragmentController;
    private final T mPreferenceController;
    private final PreferenceScreen mScreen;
    private boolean mSetPreferenceCalled;

    /**
     * Constructs a new helper. Call {@link #setPreference(Preference)} once initialization on the
     * controller is complete to associate the controller with a preference.
     *
     * @param context the {@link Context} to use to instantiate the preference controller.
     * @param preferenceControllerType the class type under test.
     */
    public PreferenceControllerTestHelper(Context context, Class<T> preferenceControllerType) {
        mMockFragmentController = mock(FragmentController.class);
        mPreferenceController = ReflectionHelpers.callConstructor(preferenceControllerType,
                ClassParameter.from(Context.class, context),
                ClassParameter.from(String.class, PREFERENCE_KEY),
                ClassParameter.from(FragmentController.class, mMockFragmentController),
                ClassParameter.from(CarUxRestrictions.class, UX_RESTRICTIONS));
        mLifecycleRegistry.addObserver(mPreferenceController);
        mScreen = new PreferenceManager(context).createPreferenceScreen(context);
    }

    /**
     * Convenience constructor for a new helper for controllers which do not need to do additional
     * initialization before a preference is set.
     *
     * @param preference the {@link Preference} to associate with the controller.
     */
    public PreferenceControllerTestHelper(Context context, Class<T> preferenceControllerType,
            Preference preference) {
        this(context, preferenceControllerType);
        setPreference(preference);
    }

    /**
     * Associates the controller with the given preference. This should only be called once.
     */
    public void setPreference(Preference preference) {
        if (mSetPreferenceCalled) {
            throw new IllegalStateException(
                    "setPreference should only be called once. Create a new helper if needed.");
        }
        preference.setKey(PREFERENCE_KEY);
        mScreen.addPreference(preference);
        mPreferenceController.setPreference(preference);
        mSetPreferenceCalled = true;
    }

    /**
     * Returns the {@link PreferenceController} of this helper.
     */
    public T getController() {
        return mPreferenceController;
    }

    /**
     * Returns a mock {@link FragmentController} that can be used to verify controller navigation
     * and stub finding dialog fragments.
     */
    public FragmentController getMockFragmentController() {
        return mMockFragmentController;
    }

    /**
     * Move the {@link PreferenceController} to the given {@code state}. This is preferred over
     * calling the controllers lifecycle methods directly as it ensures intermediate events are
     * dispatched.
     */
    public void markState(Lifecycle.State state) {
        mLifecycleRegistry.markState(state);
    }
}
