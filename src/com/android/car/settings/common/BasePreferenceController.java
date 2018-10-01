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
import android.text.TextUtils;

import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Base preference controller. The controller encapsulates the business logic associated with
 * preferences. All car settings controllers should extend this class.
 *
 * <p>Subclasses must:
 * <ul>
 * <li>Implement {@link #getAvailabilityStatus()} to define the conditions under which the
 * associated preference is available.
 * <li>Evaluate if the default behavior of {@link #canBeShownWithRestrictions(CarUxRestrictions)}
 * is sufficient and override the method if different requirements are desired.
 * <li>Override {@link #getSummary()} if the summary can change at runtime. The summary will be
 * updated each time the fragment starts or driving restrictions change.
 * <li>Override {@link #updateState(Preference)} if advanced handling of the preference state is
 * required. It is called each time the fragment starts or driving restrictions change.
 * </ul>
 *
 * <p>Most controllers can be automatically instantiated from XML. To do so, define a preference
 * and include the {@code controller} attribute in the preference tag and assign the fully
 * qualified class name.
 *
 * <p>For example:
 * <pre>{@code
 * <Preference
 *     android:key="my_preference_key"
 *     android:title="@string/my_preference_title"
 *     android:icon="@drawable/ic_settings"
 *     android:fragment="com.android.settings.foo.MyFragment"
 *     settings:controller="com.android.settings.foo.MyPreferenceController"/>
 * }</pre>
 *
 * <p>To automatically receive {@link androidx.lifecycle.OnLifecycleEvent} from
 * {@link BasePreferenceFragment}, implement {@link androidx.lifecycle.LifecycleObserver}.
 *
 * <p>In special cases, subclasses may also override {@link #displayPreference(PreferenceScreen)}
 * to obtain a reference to a preference object or do advanced layout modifications. This is
 * generally not recommended.
 */
public abstract class BasePreferenceController extends AbstractPreferenceController implements
        CarUxRestrictionsManager.OnUxRestrictionsChangedListener {

    /**
     * Denotes the availability of a setting.
     *
     * @see #getAvailabilityStatus
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AVAILABLE, CONDITIONALLY_UNAVAILABLE, UNSUPPORTED_ON_DEVICE, DISABLED_FOR_USER})
    public @interface AvailabilityStatus {
    }

    /**
     * The setting is available.
     */
    public static final int AVAILABLE = 0;

    /**
     * The setting is currently unavailable but may become available in the future. Use
     * {@link #DISABLED_FOR_USER} if it describes the condition more accurately.
     */
    public static final int CONDITIONALLY_UNAVAILABLE = 1;

    /**
     * The setting is not and will not be supported by this device.
     */
    public static final int UNSUPPORTED_ON_DEVICE = 2;

    /**
     * The setting cannot be changed by the current user.
     */
    public static final int DISABLED_FOR_USER = 3;

    protected final String mPreferenceKey;

    private CarUxRestrictions mRestrictionInfo = new CarUxRestrictions.Builder(/* reqOpt= */ true,
            CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

    /**
     * Instantiate a controller for the specified controller type and user-defined key.
     *
     * <p>This is done through reflection. Do not use this method unless you know what you are
     * doing. See class documentation for more details.
     */
    public static BasePreferenceController createInstance(Context context,
            String controllerName, String key) {
        try {
            final Class<?> clazz = Class.forName(controllerName);
            final Constructor<?> preferenceConstructor =
                    clazz.getConstructor(Context.class, String.class);
            final Object[] params = new Object[]{context, key};
            return (BasePreferenceController) preferenceConstructor.newInstance(params);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Invalid preference controller: " + controllerName, e);
        }
    }

    /**
     * Controllers should generally be instantiated from XML via reflection using
     * {@link #createInstance(Context, String, String)} from
     * {@link PreferenceControllerListHelper} if possible. To pass additional arguments see
     * {@link BasePreferenceFragment#use(Class, String)}.
     *
     * @throws IllegalArgumentException if the preference key is null or empty
     */
    public BasePreferenceController(Context context, String preferenceKey) {
        super(context);
        mPreferenceKey = preferenceKey;
        if (TextUtils.isEmpty(mPreferenceKey)) {
            throw new IllegalArgumentException("Preference key must be set");
        }
    }

    /**
     * Returns the {@link AvailabilityStatus} for the setting. This status is used to determine
     * if the setting should be shown or disabled.
     *
     * <p>Note that availability status is specific to properties of the setting and distinct
     * from availability determined by driving restrictions. See
     * {@link #canBeShownWithRestrictions(CarUxRestrictions)} to define behavior based on driving
     * restrictions.
     */
    @AvailabilityStatus
    public abstract int getAvailabilityStatus();

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public final boolean isAvailable() {
        final int availabilityStatus = getAvailabilityStatus();
        return (availabilityStatus == AVAILABLE) && canBeShownWithRestrictions(mRestrictionInfo);
    }

    @Override
    @CallSuper
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        mRestrictionInfo = restrictionInfo;
    }

    /**
     * Returns {@code true} if the preference for this controller can be shown given the {@param
     * restrictionInfo}. Defaults to {@code true}. Subclasses may override this method to modify
     * availability based on driving restrictions.
     */
    protected boolean canBeShownWithRestrictions(CarUxRestrictions restrictionInfo) {
        return true;
    }
}
