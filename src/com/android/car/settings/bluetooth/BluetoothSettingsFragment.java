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

package com.android.car.settings.bluetooth;

import static java.util.Objects.requireNonNull;

import android.bluetooth.BluetoothAdapter;
import android.car.drivingstate.CarUxRestrictions;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.widget.PagedListView;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;
import com.android.car.settings.common.CarUxRestrictionsHelper;
import com.android.car.settings.common.Logger;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * Hosts Bluetooth related preferences.
 */
public class BluetoothSettingsFragment extends BaseFragment {

    private static final Logger LOG = new Logger(BluetoothSettingsFragment.class);

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Switch mBluetoothSwitch;
    private ProgressBar mProgressBar;
    private PagedListView mDeviceListView;
    private ViewSwitcher mViewSwitcher;
    private TextView mMessageView;
    private BluetoothDeviceListAdapter mDeviceAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private LocalBluetoothManager mLocalManager;
    private boolean mShowPairedDeviceOnly;

    @Override
    @LayoutRes
    protected int getActionBarLayoutId() {
        return R.layout.action_bar_with_toggle;
    }

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.bluetooth_list;
    }

    @Override
    @StringRes
    protected int getTitleId() {
        return R.string.bluetooth_settings;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocalManager =
                LocalBluetoothManager.getInstance(getContext(), /* onInitCallback= */ null);
        if (mLocalManager == null) {
            LOG.w("Bluetooth is not supported on this device");
            getFragmentController().goBack();
            return;
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothViewModel viewModel = ViewModelProviders.of(requireActivity()).get(
                BluetoothViewModel.class);

        requireNonNull(viewModel.getBluetoothState()).observe(this, this::onBluetoothStateChanged);
        requireNonNull(viewModel.getBluetoothDiscoveryState()).observe(this,
                this::onBluetoothDiscoveryStateChanged);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBluetoothSwitch = requireActivity().findViewById(R.id.toggle_switch);
        mBluetoothSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    // If enable/disable succeeds, show states immediately to give user feedback
                    // rather than waiting for state change callbacks which have a noticeable delay.
                    if (isChecked) {
                        if (mBluetoothAdapter.enable()) {
                            showOnState();
                        }
                    } else {
                        if (mBluetoothAdapter.disable()) {
                            showOffState();
                        }
                    }
                });

        mSwipeRefreshLayout = requireActivity().findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
        mSwipeRefreshLayout.setOnRefreshListener(
                () -> {
                    mSwipeRefreshLayout.setRefreshing(false);
                    mBluetoothAdapter.cancelDiscovery();
                    mDeviceAdapter.reset();
                }
        );

        View rootView = requireNonNull(getView());
        mProgressBar = rootView.findViewById(R.id.bt_search_progress);
        mDeviceListView = rootView.findViewById(R.id.list);
        mViewSwitcher = rootView.findViewById(R.id.view_switcher);
        mMessageView = rootView.findViewById(R.id.bt_message);
    }

    @Override
    public void onStart() {
        super.onStart();
        mLocalManager.setForegroundActivity(getActivity());
        int state = mBluetoothAdapter.getState();
        mBluetoothSwitch.setChecked(
                state == BluetoothAdapter.STATE_TURNING_ON || state == BluetoothAdapter.STATE_ON);
        mDeviceAdapter = new BluetoothDeviceListAdapter(requireContext(), mLocalManager,
                getFragmentController());
        mDeviceListView.setAdapter(mDeviceAdapter);
        mDeviceAdapter.start();
        mDeviceAdapter.showPairedDeviceOnlyAndFresh(mShowPairedDeviceOnly);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDeviceAdapter.stop();
        mBluetoothAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        mBluetoothAdapter.cancelDiscovery();
        mLocalManager.setForegroundActivity(null);
    }

    /**
     * This fragment will adapt to restriction, so can always be shown.
     */
    @Override
    public boolean canBeShown(@NonNull CarUxRestrictions carUxRestrictions) {
        return true;
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
        mShowPairedDeviceOnly = CarUxRestrictionsHelper.isNoSetup(restrictionInfo);
        if (mDeviceAdapter != null) {
            mDeviceAdapter.showPairedDeviceOnlyAndFresh(mShowPairedDeviceOnly);
        }
    }

    private void onBluetoothStateChanged(@Nullable Integer state) {
        if (state == null) {
            showOffState();
            return;
        }
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                mBluetoothAdapter.setScanMode(
                        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                mBluetoothAdapter.startDiscovery();
                // Fall through.
            case BluetoothAdapter.STATE_TURNING_ON:
                if (!mBluetoothSwitch.isChecked()) {
                    // There is an edge case where the adapter may be turning on (BLE) but
                    // returns the off state when this fragment starts. Disable the adapter so
                    // that we don't show a state that is inconsistent with the switch or toggle
                    // the switch state without user input.
                    mBluetoothAdapter.disable();
                    return;
                }
                showOnState();
                break;
            default:
                showOffState();
                mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void onBluetoothDiscoveryStateChanged(@Nullable Boolean isDiscovering) {
        if (isDiscovering != null) {
            mProgressBar.setVisibility(isDiscovering ? View.VISIBLE : View.GONE);
        }
    }

    private void showOffState() {
        mProgressBar.setVisibility(View.GONE);
        if (mViewSwitcher.getCurrentView() != mMessageView) {
            mViewSwitcher.showNext();
        }
    }

    private void showOnState() {
        mProgressBar.setVisibility(View.VISIBLE);
        if (mViewSwitcher.getCurrentView() != mSwipeRefreshLayout) {
            mViewSwitcher.showPrevious();
        }
    }
}
