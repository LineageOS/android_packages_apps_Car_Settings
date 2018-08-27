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
 * limitations under the License.
 */

package com.android.car.settings.common;

import static java.util.Objects.requireNonNull;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.android.car.settings.R;

/**
 * Base fragment for setting activity.
 */
public abstract class BaseFragment extends Fragment {

    /**
     * For indicating a fragment is running in Setup Wizard
     */
    public static final String EXTRA_RUNNING_IN_SETUP_WIZARD = "extra_running_in_setup_wizard";

    /**
     * Controls the transition of fragment.
     */
    public interface FragmentController {
        /**
         * Launches fragment in the main container of current activity.
         */
        void launchFragment(BaseFragment fragment);

        /**
         * Pops the top off the fragment stack.
         *
         * @return {@code false} if there's no stack to pop, {@code true} otherwise
         */
        void goBack();

        /**
         * Shows a message that current feature is not available when driving.
         */
        void showDOBlockingMessage();
    }

    /**
     * Provides current CarUxRestrictions.
     */
    public interface UXRestrictionsProvider {

        /**
         * Fetches current CarUxRestrictions
         */
        @NonNull
        CarUxRestrictions getCarUxRestrictions();
    }

    /**
     * Assume The activity holds this fragment also implements the FragmentController.
     * This function should be called after onAttach()
     */
    public final FragmentController getFragmentController() {
        return (FragmentController) getActivity();
    }

    /**
     * Assume The activity holds this fragment also implements the UXRestrictionsProvider.
     * This function should be called after onAttach()
     */
    protected final CarUxRestrictions getCurrentRestrictions() {
        return ((UXRestrictionsProvider) getActivity()).getCarUxRestrictions();
    }

    /**
     * Checks if this fragment can be shown or not given the CarUxRestrictions. Default to
     * {@code false} if UX_RESTRICTIONS_NO_SETUP is set.
     */
    protected boolean canBeShown(@NonNull CarUxRestrictions carUxRestrictions) {
        return !CarUxRestrictionsHelper.isNoSetup(carUxRestrictions);
    }

    /**
     * Notifies the fragment with the latest CarUxRestrictions change.
     */
    protected void onUxRestrictionChanged(@NonNull CarUxRestrictions carUxRestrictions) {
    }

    /**
     * Returns the layout id to use with the {@link ActionBar}. Subclasses should override this
     * method to customize the action bar layout. The default action bar contains a back button
     * and the title.
     */
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar;
    }

    /**
     * Returns the layout id of the current Fragment.
     */
    @LayoutRes
    protected abstract int getLayoutId();

    /**
     * Returns the string id for the current Fragment title. Subclasses should override this
     * method to set the title to display. Use {@link #setTitle(CharSequence)} to update the
     * displayed title while resumed. The default title is the Settings Activity label.
     */
    @StringRes
    protected int getTitleId() {
        return R.string.settings_label;
    }

    /**
     * Should be used to override fragment's title. This should only be called after
     * {@link #onActivityCreated(Bundle)}.
     *
     * @param title CharSequence to set as the new title.
     */
    protected final void setTitle(CharSequence title) {
        TextView titleView = requireActivity().findViewById(R.id.title);
        titleView.setText(title);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof AppCompatActivity)) {
            throw new IllegalStateException("Must attach to an AppCompatActivity");
        }
        if (!(getActivity() instanceof FragmentController)) {
            throw new IllegalStateException("Must attach to a FragmentController");
        }
        if (!(getActivity() instanceof UXRestrictionsProvider)) {
            throw new IllegalStateException("Must attach to a UXRestrictionsProvider");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        @LayoutRes int layoutId = getLayoutId();
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        @LayoutRes int actionBarLayoutId = getActionBarLayoutId();
        ActionBar actionBar = requireNonNull(
                ((AppCompatActivity) getActivity()).getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setCustomView(actionBarLayoutId);
        actionBar.setDisplayShowCustomEnabled(true);
        // make the toolbar take the whole width.
        Toolbar toolbar = (Toolbar) actionBar.getCustomView().getParent();
        toolbar.setPadding(0, 0, 0, 0);

        requireActivity().findViewById(R.id.action_bar_icon_container).setOnClickListener(
                v -> onBackPressed());
        String title = getString(getTitleId());
        TextView titleView = requireActivity().findViewById(R.id.title);
        titleView.setText(title);
    }


    @Override
    public void onStart() {
        super.onStart();
        onUxRestrictionChanged(getCurrentRestrictions());
    }

    /**
     * Allow fragment to intercept back press and customize behavior.
     */
    protected void onBackPressed() {
        getFragmentController().goBack();
    }
}
