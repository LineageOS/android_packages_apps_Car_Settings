# CarSettings Tests

CarSettings tests are organized under various conveniently named directories.
For additional context, please refer to go/carsettingsunittests.

## multivalent

These tests are independent of any particular device or deviceless SDK and
classes. They compile against just android and unit testing libraries. These
tests can be run on a device, emulator, or without any device using mock SDKs at
runtime.

Our goal is to increase the number of multivalent tests as they eliminate code
duplication, reduce maintenance burden, and are portable across device and
deviceless runs.

Run these without a device (Robolectric) using following command.

```
$ atest CarSettingsMultivalentRoboTests
```

Or run with a device (physical or virtual) using following command.

```
$ atest CarSettingsMultivalentDeviceTests
```

## deviceless

These tests are purely deviceless and are not guaranteed or tested on a device
or emulator due to a direct dependency on mock SDKs or other legacy reasons.

Ideally, these tests should be rare, and any tests in this category should be
migrated to multivalent whenever possible.

```
$ atest CarSettingsDevicelessRoboTests
```

## deviceonly

These are non-multivalent device-only (instrumentation tests) due to legacy
reasons and/or due to any limitations (e.g. static mocking support) that
prevents them from migrating to multivalent.

However, with the availability of deviceless SDKs (e.g. Robolectric or
Ravenwood) and better support, these tests should also be migrated to
multivalent to make them portable across devices as well as deviceless runs.

```
$ atest CarSettingsDeviceOnlyTests
```
