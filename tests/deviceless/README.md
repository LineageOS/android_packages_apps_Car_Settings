# CarSettings Deviceless Tests

Unit test suite for Settings, SettingsLib using Robolectric MOCK SDK.

## Why they exist

These deviceless tests were previously developed using Robolectric exclusively.
Since then, they have undergone significant refinement, but they still exhibit
direct references and reliance on Robolectric-specific concepts like shadows.
While deviceless tests are valuable, solely relying on them limits their utility
and portability compared to bivalent or multivalent tests that encompass both
device-based and deviceless testing.

Ultimately, these tests should be migrated to multivalent tests to facilitate
their utilization in both deviceless and device-based testing environments.

## How to run

Currently, these tests utilize and rely on Robolectric (Mock SDK for app
testing) to execute without the requirement of a device or emulator. They
operate solely on the host and employ Robolectric to simulate SDK calls through
the use of Shadows (Robolectric-specific concepts).

```
$ atest CarSettingsDevicelessRoboTests
```

## What about other deviceless SDKs

Similar to Robolectric, there exists Ravenwood, which employs a marginally
distinct approach to running deviceless tests and is widely regarded as more
appropriate for platform testing (source: go/ravenwood-readme).

At present, there are no Ravenwood tests for CarSettings.
