# CarSettings Multivalent Tests

Unit test suite for Settings, SettingsLib with device and deviceless
capabilities either using Robolectric (or Ravenwood).

## Why Multivalent

There are called multivalent because the same tests can run in mock SDK or real
device/emulator SDK. Writing tests once and being able to run using different
SDKs help eliminate duplication and flakiness.

Deviceless tests run faster and are more stable. They provide more reliable and
quick feedback during development and help catch any regressions early.

## How to run

To run the multivalent Robolectric tests.

```
$ atest CarSettingsMultivalentRoboTests
```

To run the multivalent Device tests.

```
$ atest CarSettingsMultivalentDeviceTests
```
