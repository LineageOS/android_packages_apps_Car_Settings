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

package com.android.car.settings.testutils;

import android.app.ActivityThread;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.os.RemoteException;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.lang.reflect.Proxy;

@Implements(ActivityThread.class)
public class ShadowActivityThread {

    @Implementation
    protected static IPackageManager getPackageManager() {
        ClassLoader classLoader = ShadowActivityThread.class.getClassLoader();
        Class<?> iPackageManagerClass;
        try {
            iPackageManagerClass = classLoader.loadClass("android.content.pm.IPackageManager");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return (IPackageManager) Proxy.newProxyInstance(
                classLoader, new Class[]{iPackageManagerClass}, (proxy, method, args) -> {
                    if (method.getName().equals("getApplicationInfo")) {
                        String packageName = (String) args[0];
                        int flags = (Integer) args[1];
                        try {
                            return RuntimeEnvironment.application
                                    .getPackageManager()
                                    .getApplicationInfo(packageName, flags);
                        } catch (PackageManager.NameNotFoundException e) {
                            throw new RemoteException(e.getMessage());
                        }
                    } else if (method.getName().equals("getInstalledApplications")) {
                        int flags = (Integer) args[0];
                        return new ParceledListSlice<>(RuntimeEnvironment.application
                                .getPackageManager()
                                .getInstalledApplications(flags));
                    }
                    throw new UnsupportedOperationException("sorry, not supporting " + method
                            + " yet!");
                }
        );
    }
}
