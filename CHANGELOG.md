# Changelog

## 2026-08-22 11:05  (v0.3.0)
- Clock face enlarged: cards go from 558x684 to 580x684 canvas units, glyphs
  from 424 to 452, gap tightened to 20. Sized to the width left over once both
  side hints are accounted for.
- Calendar panel added on the left, mirroring weather on the right. Swipe right
  to open, left to close. Shows today's date and what the panel is waiting for;
  no invented appointments.
- Kiosk mode is on. The HOME alias now ships enabled, because Android 13 forbids
  shell from enabling a third-party component, and it is set as the default home
  activity. Home returns to the clock and the app cannot be exited by accident.
- Back closes an open panel and otherwise does nothing. It used to finish the
  activity, which is how the app got exited last night with no obvious way back.
- Verified on device: Home lands on the clock, five consecutive Back presses do
  not leave it, all four panels open and close.

### Overnight soak result
- Alarm fired at 07:45 and re-armed itself for Sunday. Exact alarms held.
- Device did not reboot (18h uptime). The app process restarted at ~07:30,
  which was the wake light alarm recovering it after the app was exited by hand
  the night before. Not a Go process kill.
- Battery reached 100 and sat at 20.0C. It was unplugged at some point overnight
  and was discharging from 100 by morning.
- Timezone corrected to Central on the tablet.


## 2026-08-22 10:10  (v0.2.2)
- Sleep sounds are now thirteen: the eight synthesised voices plus five recorded
  loops (Rain 2, Ocean 2, Ocean 3, Woods, Wind). `SleepVoice` is the union the
  Sounds panel renders; `SleepAudioService` branches on it and both paths walk
  the same fade curve. `PROJECT_OVERVIEW.md`'s "no audio assets shipped" claim is
  gone, because it is no longer true.
- Alarm tones are real. Each alarm stored one of six names before this and every
  one of them played the device default, so the picker was decorative. Now:
  Clock bell, Chimes, Birds 1, Birds 2, Device default. Old stored names migrate
  to Clock bell through `AlarmTone.byName` rather than throwing on an alarm that
  is trying to ring.
- Added `audio/LoopPlayer.kt`. `MediaPlayer.isLooping` restarts the decoder at
  the loop point, which is an audible tick in a quiet room, so it keeps two
  players and hands off with `setNextMediaPlayer`. Alarms pass `USAGE_ALARM` so
  they still sound through Do Not Disturb.
- Where a synthesised voice and a recording cover the same ground they are
  numbered instead of given competing prose names: "Rain 1" and "Rain 2" rather
  than "Rain on glass" and "Rain". Same for Ocean 1 through 3 and Birds 1 and 2.
- The Sounds grid went from two rows of four to three rows of five. Thirteen
  cards do not fit four rows on an 800px canvas, so the card is shorter (132 to
  112) and narrower. Recorded loops carry a RECORDED tag in the existing tag slot.
- The two CC BY 4.0 credits now render at the foot of the Sounds panel, driven by
  `AUDIO_CREDITS`. That is a licence condition, not a courtesy.
- `s_noise` changed format from a bare `NoiseType` name to a `synth:`/`loop:`
  key. The old form is still parsed, so upgrading keeps the device's setting.
- Verified on the AlarmHub_Go emulator, not assumed: a MediaPlayer at 44100 Hz
  stereo matching `SleepAudioService.SAMPLE_RATE`; the gapless handoff landing
  exactly on the 45s loop point of Wind with no moment of zero started players;
  four consecutive handoffs on the 8.4s clock bell with the ring state stable;
  and the alarm tone running on `USAGE_ALARM`.
- APK is 14 MB, of which 5.2 MB is audio, stored rather than deflated.

## 2026-08-21 21:35  (v0.2.2)
Four usability bugs found by actually using the tablet.

- Night dimming is now driven by idleness rather than a 15 second timer, and it
  never applies while a panel is open. The old behaviour dimmed the screen out
  from under an active hand, and once it did there was no way to undo it from
  inside a panel: the tap-to-brighten handler only fired on unconsumed taps, and
  every panel control consumes them. Interaction is now observed on the Initial
  pointer pass, so any touch anywhere counts. Sixty seconds of no touches on the
  clock brings the dim back.
- While interacting at night the scrim comes off entirely rather than being
  thinned to 25%, and brightness lifts to 0.38.
- The volume control only answered a discrete tap. Dragging it was picked up by
  the panel navigation on the parent instead, so it read as broken. It now
  consumes from the first press and tracks the finger, and its touch target is
  44 canvas units tall instead of the 4 the visible track suggests.
- "Swipe down to close" did nothing from the alarm list, because the scroll
  container consumed every vertical drag inside it. The list now only scrolls
  once there are more than four alarms, which is past the point where it fits.
- Files affected: ui/Light.kt, ui/SoundsPanel.kt, ui/AlarmsPanel.kt,
  MainActivity.kt


## 2026-08-21 20:25
- Added `audio/`: nine seamless, level-matched OGG Vorbis loops built from
  Freesound source, plus `prep_sounds.sh` that regenerates them and `README.md`
  carrying the licence record. 5.2 MB total. Nothing is wired into the app; this
  is staging only, and `PROJECT_OVERVIEW.md`'s "no audio assets shipped" is
  still accurate.
- Loops are crossfade-wrapped with equal-power fades rather than cut, and gain
  is applied as a static offset. Both choices exist to keep the join inaudible:
  a linear fade dips at the seam and a dynamic normaliser drifts across it.
- Output is 44100 Hz stereo to match `SleepAudioService.SAMPLE_RATE`, and Vorbis
  rather than Opus because `minSdk` 26 predates Android's Opus-in-`.ogg` support
  (API 29).
- Rejected one source: freesound 263995 is CC BY-NC 4.0, which does not belong
  in an app that may reach a store listing. Two CC0 ocean recordings cover it.
- Two of the nine are CC BY 4.0 and need a visible in-app credit before release:
  `sleep_rain` (lebaston100) and `wake_tubular_chimes` (sonocrea). The other
  seven are CC0. Table in `audio/README.md`.
- Caught during verification, worth recording: the autumn woods source has a
  lone transient at 147.63s sitting 27 dB above the recording's typical peak.
  Peak-safe gain therefore left the first build at -37.5 LUFS, inaudible. The
  window now starts at 165s and it measures -20.31 LUFS.
- Added the `AlarmHub_Go` AVD and `run_go_emulator.sh`. The profile mirrors the
  measured PRITOM L8 where it can: 1280x800 at 213dpi, landscape, no ambient
  light sensor, so the fixed-night-dim fallback gets exercised instead of the
  sensor path. Verified booted: density and size match exactly and no
  `android.sensor.light` appears in the sensor list.
- Recorded a limit rather than papering over it: `ro.config.low_ram` cannot be
  set on an emulator. Both `emulator -prop` and `adb shell setprop` refuse it as
  a build-time read-only property. `ActivityManager.isLowRamDevice` is therefore
  false on this AVD and the low-RAM crossfade path is NOT covered by it. That
  path still has to be tested on the real L8. The AVD is also API 36 / arm64
  against the device's API 33 / armeabi-v7a.

- Open: whether to become a hybrid (keep `NoiseGen` for noise, add recordings
  for surf, woods, birdsong and bells, which synthesis cannot do). Not decided.

## 2026-08-21 19:35  (v0.2.1)
- Fixed the stuck minutes reported from the tablet. In the low-RAM crossfade
  path the bottom flap's backing layer was pinned to the previous value, so once
  the crossfade overlay stopped drawing at p == 1 the old digit was left showing
  underneath. Only the top half ever updated. The backing now settles on the
  current value. Verified across two live minute turns on the device.
- Fixed system bars reappearing after any edge swipe and then lingering. An
  insets listener re-hides them whenever they report themselves visible.
- BootReceiver now also handles TIMEZONE_CHANGED and TIME_SET. Alarms are armed
  as absolute epoch times derived from a wall clock, so changing the zone left
  them an hour off until something else happened to reschedule.
- Found: the tablet's auto-detected timezone was America/New_York while the
  device is in Central. Auto-detection is now disabled over adb, but shell lacks
  SUGGEST_MANUAL_TIME_AND_ZONE so the zone itself has to be set on the tablet.


## 2026-08-21 19:05
- First run on the real PRITOM L8 (`L8_C01`), paired over wireless debugging.
- Device facts that matter: Android 13 / API 33, `ro.config.low_ram=true`,
  1.9GB RAM, armeabi-v7a, Unisoc SP7731E, 1280x800 @ 213dpi. No light sensor,
  only an accelerometer, so ambient auto-dim falls back to the fixed value.
- `USE_EXACT_ALARM` is auto-granted here: alarms arm with
  `exactAllowReason=policy_permission`. No permission dialog needed.
- Added a release build type (R8 on, signed with the debug key since this is
  sideloaded onto one tablet). APK drops from 8.5MB to 1.1MB.
- Perf: ClockPane now takes pre-derived strings instead of a LocalDateTime, so
  the two large cards skip the every-second tick. Resting render went from
  229 frames per 75s to 10.
- Perf: panels slide via graphicsLayer translation instead of offset, so the
  compositor moves an existing layer rather than redrawing 1280x800 of UI per
  frame. Median frame time during transitions fell from 81ms to 46ms.
- Files affected: app/build.gradle.kts, app/proguard-rules.pro,
  ui/ClockPane.kt, MainActivity.kt


## 2026-08-21 18:12
- Rebuilt the interface against the Claude Design source `Flip Clock Tablet.dc.html`.
- Clock is now two large split-flap cards with the meridiem in the hour card's
  corner, replacing four digits, a colon and a seconds bank.
- Navigation is four-way: swipe up for alarms, down for sounds, left for weather.
- Multiple alarms, each with its own repeat days, tone, wake light and lead time.
- Sleep sounds expanded from three voices to the design's eight, all synthesised.
- Added a black scrim for real night darkness, with ambient-light auto-dim when
  the panel has a light sensor.
- Android Go tuning: crossfade instead of rotation on low-RAM devices.
- Weather panel renders the design's chrome but shows no invented data, since
  this build still has no network permission.
- Fixed: per-event snapTo calls cancelled the settle animation, so panels could
  strand half open and never commit the new view.
- Fixed: the toggle collapsed to just its knob when off, so the two states did
  not read as the same control.
- Fixed: sleep audio kept playing underneath a ringing alarm.
- Files affected: whole ui package, data/Prefs.kt, audio/NoiseGen.kt,
  alarm/AlarmScheduler.kt, alarm/AlarmReceiver.kt, alarm/BootReceiver.kt,
  MainActivity.kt, AlarmHubApp.kt

## 2026-08-21 17:25
- MVP1 complete and verified on a 1280x800 emulator matching the PRITOM L8.
- Fixed: snooze target was not reactive, so the clock advertised the original
  alarm time instead of the 9 minute snooze.
- Fixed: sunrise ramp fired during a snooze, flooding the room with light.
- Fixed: alarm overlay used an alpha-blended gradient centre, letting the clock
  pane show through the alarm screen.
- Fixed: secondary text became unreadable at the bright end of the sunrise ramp.
- Fixed: stepper arrows swallowed taps short enough to toggle the press state
  inside a single frame.
- Fixed: clock row sized from height alone and overflowed a 16:10 panel.
- Fixed: digit ink sat above the flap seam, leaving the lower flap empty.
- Files affected: whole project (initial build).

## 2026-08-21 16:42
- Project scaffolded: Kotlin, Jetpack Compose, AGP 8.7.3, Gradle 8.11.1,
  minSdk 26, targetSdk 34.
- Files affected: settings.gradle.kts, build.gradle.kts, app/build.gradle.kts,
  app/src/main/AndroidManifest.xml
