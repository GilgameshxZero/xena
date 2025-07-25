# xena

Freeform & PDF-backed notes for Android and Windows, compatible with e-ink tablets (e.g. Onyx Boox), with a focus on the SVG ecosystem.

## Android

### General dependencies

1. JDK 17.
2. Android build, command-line, and platform tools.

Dependencies are automatically installed by `make`. The first time running `make`, there may be Android licenses to manually accept.

The JDK is used by VSCode Java extensions. Without the JDK, formatting will fail.

`JAVA_HOME` should be set to either the JDK path in this project, or a system-wide JDK installation. It may be overridden with `java.jdt.ls.java.home` for just this project. Unfortunately, this setting is relative to not this project path, and may not be set in VC.

### Connecting with `adb`

`adb` is required to run apps on any attached devices. To attach to a LAN device over TCP/IP:

1. Attach the devices with USB, and enable USB debugging on the device.
2. `adb devices` should show the attached device.
3. `adb tcpip 5555`
4. `adb connect IP`, where `IP` is replaced with the LAN IP of the device.
5. Detach the device, and `adb devices` should still show the device.

The downloaded dependency `adb` can be run with `make adb ARGS=""`.

### Resources

1. <https://www.hanshq.net/command-line-android.html>

## Windows
