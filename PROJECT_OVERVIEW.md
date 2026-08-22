# AlarmHub

A flip-clock alarm and bedside hub for an Android tablet. Built for a PRITOM L8
(8 inch, 1280x800 IPS) that sits on a dock and runs one app, all the time.

MVP1 is deliberately **fully offline**. The built APK holds no `INTERNET`,
`ACCESS_*_LOCATION`, `CAMERA`, or `RECORD_AUDIO` permission, so it cannot reach
the network even if something in it wanted to. Calendar and weather arrive in
MVP2 and will be the point at which network access is introduced.

The interface follows the Claude Design source `Flip Clock Tablet.dc.html`,
transcribed in that file's 1280x800 canvas units and scaled once at runtime.

## What it does

| Feature | Where it lives |
| --- | --- |
| Two-card split-flap clock, 12h or 24h | `ui/FlipCard.kt`, `ui/ClockPane.kt` |
| Multiple alarms, repeat days, per-alarm tone | `alarm/AlarmScheduler.kt`, `ui/AlarmsPanel.kt` |
| Warm wake light ramp before each alarm | `ui/Light.kt`, `ui/RingScreen.kt` |
| Night dimming, with ambient-light auto-dim | `ui/Light.kt` |
| Thirteen sleep sounds, synthesised and recorded, with a fade-out timer | `audio/NoiseGen.kt`, `audio/LoopPlayer.kt`, `audio/SleepAudioService.kt` |

Four-way navigation with the clock at rest in the centre:

```
            [ Sounds ]
                 ^  (swipe down)
[ Clock ]  ->  (swipe left)  ->  [ Weather ]
                 v  (swipe up)
            [ Alarms ]
```

## Architecture

Single activity, single process, no database, no network layer.

```
AlarmHubApp            Application. Owns Prefs, creates notification channels,
                       re-arms alarms on every cold start.
  |
  +- data/Prefs        SharedPreferences behind StateFlows, alarms as JSON.
  |                    The only state store.
  |
  +- MainActivity      Owns the window and the four-way navigation, applies
  |    |               brightness, reads the ambient light sensor.
  |    +- ui/Design    Canvas-unit scaling (du/su) and the design's tokens.
  |    +- ui/Light     Pure function: (now, nextFire, config, lux) -> brightness
  |    |               and scrim. No timers, no state machine.
  |    +- ui/*Panel    Dumb composables driven by Prefs flows.
  |
  +- alarm/            AlarmScheduler picks the earliest of all enabled alarms
  |                    plus any snooze, and arms two exact alarms (wake light,
  |                    ring). AlarmReceiver persists a firing id and fires a
  |                    full-screen intent. AlarmRinger fades the tone in over
  |                    30s. BootReceiver re-arms after reboot.
  |
  +- audio/            SleepAudioService plays one of thirteen voices. Eight are
                       synthesised by NoiseGen into an AudioTrack on a worker
                       thread; five are recorded loops in res/raw, played through
                       LoopPlayer. Both walk the same fade curve.
```

### Decisions worth remembering

**`setAlarmClock()`, not `setExact()`.** It is the only exact-alarm variant Doze
will never defer, and it is what puts the alarm icon in the status bar. The
manifest declares `USE_EXACT_ALARM`, which Android grants automatically to apps
whose core function is an alarm clock.

**The alarm state is persisted, not held in memory.** `Prefs.firingAt` is
written to disk before the notification is posted, so a process that gets killed
mid-alarm still shows the ring screen and still rings when it restarts. This is
tested: force-stopping the app during an alarm and relaunching resumes it.

**Window brightness, not system brightness.** `window.attributes.screenBrightness`
needs no `WRITE_SETTINGS` permission and reverts automatically when the app
loses focus, so the app can never leave the tablet stuck at 4% brightness.

**The light is a pure function of the clock.** `computeLight()` takes the current
time and the next alarm time and returns a palette and a brightness. There is no
"sunrise in progress" state to get out of sync. The scheduled sunrise alarm
exists only to bring the activity forward, not to drive the animation.

**A snooze suppresses the ramp.** Running a 15 minute wake light for a 9 minute
snooze would floodlight the room seconds after the user asked for more sleep.

**Drag state and settle animation are separate.** While a finger is down the raw
drag value drives the panels; the `Animatable` only runs the release settle.
Driving one `Animatable` from both let a per-event `snapTo` cancel the settle
mid-flight, which stranded panels half open and never committed the new view.

**Noise is synthesised; nature is recorded.** `NoiseGen` builds the eight
noise-shaped voices sample by sample from a brown generator, a Paul Kellet pink
filter, one-pole lowpasses and slow modulators. Left and right use different
seeds so the stereo image is decorrelated rather than centred in your head. For
noise that is still the better answer outright: nothing to ship and nothing that
can repeat over an eight hour night.

What synthesis cannot fake is surf with real wave rhythm, a wood at dusk,
birdsong, or a struck bell, so those five are recordings. They cost 5.2 MB of
APK and two credit lines, which is the price of having the sounds a bedside
clock most wants.

**Every recording is a loop, so every recording is crossfade-wrapped.** A cut
loop clicks, and a click at 3am is the whole problem. `audio/prep_sounds.sh`
folds each file's tail back onto its head with equal-power fades and applies
loudness as a static gain, never a dynamic one, because a normaliser that moves
would drift across the seam. Loop lengths run 45 to 120 seconds: rain and wind
are stationary and undetectable short, surf and birdsong carry a rhythm the ear
tracks and need longer before the pattern announces itself.

**The join is gapless in the player too.** `MediaPlayer.isLooping` restarts the
decoder at the loop point, so `LoopPlayer` keeps two players and hands off with
`setNextMediaPlayer`, the platform's own gapless mechanism. Verified on device:
across a 45s loop and four consecutive 8.4s loops there was never a moment with
no player started.

**Darkness comes from a scrim, not the backlight.** A panel at its minimum
backlight is still far too bright at 3am, so night mode paints a black overlay
on top at up to 92% opacity. The same control run in reverse is what opens the
wake light, which is why the ramp can start from genuine darkness.

**Tuned for Android Go rather than rewritten for it.** The device reports
`ActivityManager.isLowRamDevice`, and that flag turns on crossfade instead of
rotation for the card change. The design itself does most of the work: two cards
with no seconds means one animation per minute instead of sixty, which is the
single biggest GPU saving available.

**One flip per minute, by design.** The seconds bank from the first prototype
was dropped when the design landed. It animated 60 times a minute forever, and
on a weak GPU that was the largest continuous cost in the app.

## Building

Needs JDK 17 and the Android SDK. There is no Android Studio dependency.

```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` points at `~/Library/Android/sdk`.

## Running it as the tablet's home screen

The manifest contains a disabled `HomeAlias`. Turn it on when the tablet is
ready to become an appliance:

```
adb shell pm enable com.kindcode.alarmhub/.HomeAlias
```

Disable it again to get the stock launcher back:

```
adb shell pm disable-user com.kindcode.alarmhub/.HomeAlias
```

## Target hardware, measured

PRITOM L8, reported as `L8_C01`:

| | |
| --- | --- |
| OS | Android 13, API 33 |
| Go edition | yes, `ro.config.low_ram=true` |
| RAM | 1.9 GB |
| ABI | armeabi-v7a (32-bit) |
| SoC | Unisoc SP7731E |
| Display | 1280x800 IPS at 213dpi |
| Sensors | accelerometer only, **no ambient light sensor** |

Consequences baked into the app: the low-RAM flag turns on crossfade instead of
rotation for card changes, `USE_EXACT_ALARM` is auto-granted at API 33 so alarms
arm with no permission dialog, and the ambient auto-dim setting has nothing to
read from, so it falls back to the fixed night dim value.

Frame timing on the device, release build: resting draws 10 frames per 75
seconds, so the clock is effectively idle between minutes. Panel transitions run
around 46ms per frame, which is roughly 22fps for the 440ms slide.

## Verified on

An emulator configured to match the target hardware exactly: 1280x800 at 213dpi,
landscape, API 36, and then on the tablet itself over wireless debugging. Exercised on device: clock render and flip, all four
navigation gestures, the alarm list and editor, all eight sound voices through
the foreground service, the wake screen, snooze rescheduling, stop, and a
cold start while an alarm was still ringing.

## Not yet built

- Weather data (the panel is standing by; planned: Open-Meteo, no API key,
  coordinates stored as a fixed setting so no location permission is needed)
- Calendar (planned: CalDAV via DAVx5, read through `CalendarContract`)
- Home Assistant control and camera streams
- Offline voice commands (planned: openWakeWord plus Vosk, fully on-device)
