# Tools

`build-and-install.sh` and `build-and-install.ps1` build the debug APK and install it using ADB. They do not change TV settings or grant capture permission. Use an explicit ADB serial when more than one device is connected; Android will ask for screen-capture consent on the TV when you start sync.

```sh
./tools/build-and-install.sh              # single authorized device
./tools/build-and-install.sh SERIAL       # selected authorized device
```

```powershell
./tools/build-and-install.ps1
./tools/build-and-install.ps1 -Serial SERIAL
```

Requirements: JDK 17+, Android SDK 35 and platform-tools (`adb`) on PATH. For checks without installation use `./gradlew testDebugUnitTest lintDebug assembleDebug`.
