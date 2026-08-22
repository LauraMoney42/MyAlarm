#!/usr/bin/env bash
# Boots the AlarmHub_Go AVD, which mirrors the measured PRITOM L8 where an
# emulator can: 1280x800 at 213dpi, landscape, and no ambient light sensor, so
# ui/Light.kt takes its fixed-night-dim fallback rather than the sensor path.
#
# What this AVD CANNOT do, tested and confirmed:
#
#   ro.config.low_ram cannot be set. Neither `emulator -prop ro.config.low_ram=true`
#   nor `adb shell setprop` will set it ("Failed to set property"): it is a
#   build-time read-only property baked into the system image. That means
#   ActivityManager.isLowRamDevice is FALSE here, so the low-RAM crossfade path
#   in FlipCard is NOT exercised on this emulator. Test that on the real L8.
#
#   The image is API 36 / arm64-v8a. The real unit is API 33 / armeabi-v7a.
#   Installing an android-33 image would close the API gap; the 32-bit ABI gap
#   cannot be closed on an arm64 Mac.
#
# RAM lands at ~1.97 GB regardless of hw.ramSize, which happens to match the
# real unit's 1.9 GB, so that part is right by accident rather than by config.
set -euo pipefail
EMU="$HOME/Library/Android/sdk/emulator/emulator"
exec "$EMU" -avd AlarmHub_Go -no-boot-anim -no-snapshot-save "$@"
