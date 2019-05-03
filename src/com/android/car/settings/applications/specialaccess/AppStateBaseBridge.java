/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.settings.applications.specialaccess;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.settingslib.applications.ApplicationsState;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Common base class for bridging information to {@link ApplicationsState.AppEntry#extraInfo}.
 * Subclasses should implement {@link #loadExtraInfo(List)} and populate the fields in the given
 * {@link ApplicationsState.AppEntry} instances.
 */
public abstract class AppStateBaseBridge {

    /** Callback for receiving events from the bridge. */
    public interface Callback {
        /**
         * Called when the bridge has finished updating all
         * {@link ApplicationsState.AppEntry#extraInfo} in {@link ApplicationsState}.
         */
        void onExtraInfoUpdated();
    }

    private final ApplicationsState.Session mSession;
    private final Callback mCallback;
    private final BackgroundHandler mBackgroundHandler;
    private final MainHandler mMainHandler;

    private final ApplicationsState.Callbacks mSessionCallbacks =
            new ApplicationsState.Callbacks() {
                @Override
                public void onRunningStateChanged(boolean running) {
                    // No op.
                }

                @Override
                public void onPackageListChanged() {
                    mBackgroundHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ALL);
                }

                @Override
                public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                    // No op.
                }

                @Override
                public void onPackageIconChanged() {
                    // No op.
                }

                @Override
                public void onPackageSizeChanged(String packageName) {
                    // No op.
                }

                @Override
                public void onAllSizesComputed() {
                    // No op.
                }

                @Override
                public void onLauncherInfoChanged() {
                    // No op.
                }

                @Override
                public void onLoadEntriesCompleted() {
                    mBackgroundHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ALL);
                }
            };

    private boolean mIsStarted;

    public AppStateBaseBridge(ApplicationsState applicationsState, Callback callback) {
        mSession = applicationsState.newSession(mSessionCallbacks);
        mCallback = callback;
        // Run on the same background thread as the ApplicationsState to make sure updates don't
        // conflict.
        mBackgroundHandler = new BackgroundHandler(new WeakReference<>(this),
                applicationsState.getBackgroundLooper());
        mMainHandler = new MainHandler(new WeakReference<>(this));
    }

    /**
     * Starts loading the information in the background. When loading is finished, the {@link
     * Callback} will be notified on the main thread.
     */
    public void start() {
        mIsStarted = true;
        mBackgroundHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_ALL);
        mSession.onResume();
    }

    /**
     * Stops any pending loading. In progress loading may still complete, but no {@link Callback}
     * notifications will be delivered.
     */
    public void stop() {
        mIsStarted = false;
        mBackgroundHandler.removeMessages(BackgroundHandler.MSG_LOAD_ALL);
        mSession.onPause();
    }

    /**
     * Cleans up internal state when this bridge will no longer be used.
     */
    public void destroy() {
        mSession.onDestroy();
    }

    /**
     * Updates the {@link ApplicationsState.AppEntry#extraInfo} of the given {@code entry}. When
     * loading is finished, the {@link Callback} will be notified on the main thread.
     */
    public void forceUpdate(ApplicationsState.AppEntry entry) {
        mBackgroundHandler.obtainMessage(BackgroundHandler.MSG_FORCE_LOAD_PKG,
                entry).sendToTarget();
    }

    /**
     * Populates the {@link ApplicationsState.AppEntry#extraInfo} field on the {@code enrties} with
     * the relevant data for the subclass.
     */
    protected abstract void loadExtraInfo(List<ApplicationsState.AppEntry> entries);

    private static class BackgroundHandler extends Handler {
        private static final int MSG_LOAD_ALL = 1;
        private static final int MSG_FORCE_LOAD_PKG = 2;

        private final WeakReference<AppStateBaseBridge> mOuter;

        BackgroundHandler(WeakReference<AppStateBaseBridge> outer, Looper looper) {
            super(looper);
            mOuter = outer;
        }

        @Override
        public void handleMessage(Message msg) {
            AppStateBaseBridge outer = mOuter.get();
            if (outer == null) {
                return;
            }
            switch (msg.what) {
                case MSG_LOAD_ALL:
                    outer.loadExtraInfo(outer.mSession.getAllApps());
                    outer.mMainHandler.sendEmptyMessage(MainHandler.MSG_INFO_UPDATED);
                    break;
                case MSG_FORCE_LOAD_PKG:
                    ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) msg.obj;
                    outer.loadExtraInfo(Collections.singletonList(entry));
                    outer.mMainHandler.sendEmptyMessage(MainHandler.MSG_INFO_UPDATED);
                    break;
            }
        }
    }

    private static class MainHandler extends Handler {
        private static final int MSG_INFO_UPDATED = 1;

        private final WeakReference<AppStateBaseBridge> mOuter;

        MainHandler(WeakReference<AppStateBaseBridge> outer) {
            mOuter = outer;
        }

        @Override
        public void handleMessage(Message msg) {
            AppStateBaseBridge outer = mOuter.get();
            if (outer == null) {
                return;
            }
            switch (msg.what) {
                case MSG_INFO_UPDATED:
                    if (outer.mIsStarted) {
                        outer.mCallback.onExtraInfoUpdated();
                    }
                    break;
            }
        }
    }
}
