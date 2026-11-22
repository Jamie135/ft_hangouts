# ft_hangouts

A contacts and SMS Android app (Kotlin, min SDK 24, target/compile SDK 37).

This guide explains how to build the app and run it in the Android emulator on a **42 school machine**, with everything big (SDK, emulator images, Gradle cache) stored in `goinfre`. Your home folder has a small quota, so nothing large should end up there.

> **Note:** `goinfre` is local to each machine and may be wiped. If you change computers or it gets cleaned, redo steps 1–4. Your project files are safe if they are in your home folder or in git.

---

## 0. Variables used in this guide

On most campuses, `goinfre` is `/goinfre/$USER`. Check yours:

```sh
ls -d /goinfre/$USER ~/goinfre 2>/dev/null
```

All commands below use `$GOINFRE`. Set it to the path that exists:

```sh
export GOINFRE=/goinfre/$USER
```

---

## 1. Point every Android/Gradle folder to goinfre

By default the SDK, emulator AVDs and Gradle cache all go into `~` and can quickly use more than 10 GB. Add this to your `~/.zshrc` (or `~/.bashrc`) so it applies to every new shell:

```sh
# --- Android / ft_hangouts ---
export GOINFRE=/goinfre/$USER
export ANDROID_HOME=$GOINFRE/android-sdk          # SDK
export ANDROID_SDK_ROOT=$ANDROID_HOME             # older tools still read this
export ANDROID_USER_HOME=$GOINFRE/.android        # ~/.android (adb keys, settings)
export ANDROID_AVD_HOME=$ANDROID_USER_HOME/avd    # emulator disk images (large)
export GRADLE_USER_HOME=$GOINFRE/.gradle          # Gradle cache (large)
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
```

Then reload it:

```sh
source ~/.zshrc
mkdir -p $ANDROID_HOME $ANDROID_AVD_HOME $GRADLE_USER_HOME
```

If you already have `~/.android` or `~/.gradle` folders using space, you can delete them now.

---

## 2. Make sure a JDK (17 or newer) is available

The Android Gradle Plugin needs Java 17+.

```sh
java -version
```

If the version is lower than 17, or `java` is missing, install a JDK into goinfre (no root access needed). For example, with [Eclipse Temurin](https://adoptium.net/temurin/releases/):

```sh
cd $GOINFRE
curl -L -o jdk.tar.gz "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar xzf jdk.tar.gz && rm jdk.tar.gz
mv jdk-21* jdk
```

Add these lines to `~/.zshrc`:

```sh
export JAVA_HOME=$GOINFRE/jdk
export PATH=$JAVA_HOME/bin:$PATH
```

---

## 3. Install the Android SDK command-line tools

1. Download the **"Command line tools only"** zip for Linux from <https://developer.android.com/studio#command-line-tools-only>. Copy the current link from that page; the build number in the file name changes over time.
2. Unzip it into the layout `sdkmanager` expects (`cmdline-tools/latest/`):

```sh
cd $ANDROID_HOME
curl -L -o cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-linux-<VERSION>_latest.zip"
unzip -q cmdtools.zip && rm cmdtools.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/bin cmdline-tools/lib cmdline-tools/NOTICE.txt cmdline-tools/source.properties cmdline-tools/latest/
```

Check that it works:

```sh
sdkmanager --version
```

---

## 4. Install the SDK packages and create an emulator

Accept the licences, then install the platform tools (`adb`), the emulator, the Android 37 platform and an x86_64 system image:

```sh
yes | sdkmanager --licenses
sdkmanager "platform-tools" "emulator" \
           "platforms;android-37.0" \
           "system-images;android-37.0;google_apis_playstore;x86_64"
```

Gradle downloads the matching build-tools on its own during the first build.

> To see every available package name, run `sdkmanager --list`. If `android-37.0` isn't listed, use the newest `platforms;android-37*` and the matching system image.

Create a virtual device (AVD):

```sh
avdmanager create avd -n ft_phone_1 \
  -k "system-images;android-37.0;google_apis_playstore;x86_64" \
  -d medium_phone
```

Answer `no` when asked about a custom hardware profile. The AVD is saved in `$ANDROID_AVD_HOME` (goinfre), not in your home folder.

---

## 5. Start the emulator

```sh
emulator -avd ft_phone_1 &
```

Useful flags:

| Flag | When to use it |
|------|----------------|
| `-no-snapshot` | Do a clean cold boot (if the emulator hangs while loading a snapshot) |
| `-gpu swiftshader_indirect` | Use software rendering if the window is black or the emulator crashes on the GPU |
| `-memory 2048` | Use less RAM on a busy machine |

**Hardware acceleration (KVM):** the x86_64 emulator needs KVM to run at a usable speed. To check:

```sh
emulator -accel-check
```

If KVM isn't available, the emulator will be very slow or won't start. In that case, ask the staff or use a physical device over USB (enable *Developer options → USB debugging*). The app installs the same way.

Wait until the home screen appears, then check that `adb` can see the emulator:

```sh
adb devices
# List of devices attached
# emulator-5554   device
```

---

## 6. Build and install the app

Tell Gradle where the SDK is. `local.properties` is git-ignored and specific to each machine:

```sh
cd /path/to/ft_hangouts
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

Build the debug APK and install it on the running emulator:

```sh
./gradlew installDebug
```

The first build is slow because Gradle downloads itself, the Android Gradle Plugin and the dependencies into `$GRADLE_USER_HOME`. Later builds are fast.

Launch the app:

```sh
adb shell am start -n com.pbureera.ft_hangouts/.ui.MainActivity
```

Or tap the **ft_hangouts** icon in the emulator's app drawer.

Other useful commands:

```sh
./gradlew assembleDebug                     # build only -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat --pid=$(adb shell pidof com.pbureera.ft_hangouts)   # app logs only
adb uninstall com.pbureera.ft_hangouts      # removes the app and its database
```

---

## 7. Testing SMS between two emulators

Each emulator's "phone number" is its console port: the first one is `5554`, the second `5556`, and so on.

Create a second AVD and start both:

```sh
avdmanager create avd -n ft_phone_2 \
  -k "system-images;android-37.0;google_apis_playstore;x86_64" -d medium_phone

emulator -avd ft_phone_1 &   # number 5554
emulator -avd ft_phone_2 &   # number 5556
```

Install the app on both:

```sh
./gradlew installDebug          # installs on every connected device
```

In the app on `emulator-5554`, add a contact with phone number `5556` and send it a message. It arrives on the second emulator.

You can also fake an incoming SMS without a second emulator:

```sh
adb -s emulator-5554 emu sms send 5556 "Hello from 5556"
```

---

## 8. Alternative: set up with Android Studio (for 42 machine)

Instead of steps 3–6, you can install Android Studio into goinfre and let it install the SDK and create the emulator for you.

### 1. Download and unpack

```sh
mkdir -p /goinfre/$USER/android && cd /goinfre/$USER/android
# Copy the current Linux .tar.gz link from https://developer.android.com/studio
wget https://edgedl.me.gvt1.com/android/studio/ide-zips/2026.2.2.9/android-studio-rabbit2-patch1-linux.tar.gz
tar -xzf android-studio-*-linux.tar.gz
./android-studio/bin/studio.sh
```

### 2. Keep the big data out of your home folder

On first run, set the location in the **SDK Components Setup** to `/goinfre/$USER/android/sdk`.

The SDK and emulator images take several GB, so they won't fit in home.

Also move Gradle's cache and the emulator's AVD files:

```sh
echo 'export GRADLE_USER_HOME=/goinfre/$USER/android/gradle' >> ~/.bashrc
echo 'export ANDROID_AVD_HOME=/goinfre/$USER/android/avd' >> ~/.bashrc
```

> - If your shell is zsh (`echo $SHELL`), write these lines to `~/.zshrc` instead of `~/.bashrc`.
> - Open a new terminal (or `source` the file) **before** starting Android Studio again, so it picks up the variables.
> - This layout puts everything under `/goinfre/$USER/android/`, which is a different path from step 1. Use one layout or the other, not both. To use the command-line commands from this README with this setup, also set `ANDROID_HOME=/goinfre/$USER/android/sdk`.

### 3. Run the app

In Android Studio, **File → Open** this project folder. Studio writes `local.properties` for you. Create an emulator in **Device Manager** (for example a Medium Phone with an API 37 x86_64 image), then press **Run ▶**.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `SDK location not found` | Create `local.properties` with `sdk.dir=...` (step 6) or check `echo $ANDROID_HOME` |
| `Disk quota exceeded` in home | Something is still writing to `~`. Run `du -sh ~/.android ~/.gradle ~/.cache`, delete those folders, and check the step 1 variables are set in the current shell |
| `adb devices` shows `offline` / nothing | Run `adb kill-server && adb start-server` and wait for the emulator to finish booting |
| Emulator window black / crashes | Start it with `-gpu swiftshader_indirect -no-snapshot` |
| `Unsupported class file major version` / Java errors | `java -version` must be 17+; set `JAVA_HOME` (step 2) |
| Licence not accepted errors during build | `yes \| sdkmanager --licenses` |
| Everything is gone the next day | goinfre was wiped or you're on a different machine. Redo steps 1–4 |
