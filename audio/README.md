# Audio source material

Nothing in here is wired into the app yet. `prepared/` is a staging area, and
`PROJECT_OVERVIEW.md` still says "no audio assets shipped", which is still true.
See "Open decision" at the bottom.

## How the loops are made

`prep_sounds.sh` turns raw Freesound downloads into seamless, level-matched
OGG Vorbis loops. Re-run it any time; it is idempotent and reads from
`~/Downloads`.

Three decisions are baked into it:

**Crossfade-wrap, not a butt splice.** The tail of each segment is folded back
onto its head with equal-power (`qsin`) fades. Linear fades dip in level at the
join on uncorrelated material, and a level dip at the loop point is exactly the
kind of thing that surfaces a sleeper.

**Static gain, not dynamic normalisation.** Loudness is measured in a first pass
and applied as a fixed dB offset. A dynamic normaliser drifts across the seam
and makes the join audible. Sleep loops get no limiter at all for this reason.
Wake tones may exceed peak-safe gain by up to 6 dB with `alimiter` catching the
transients, because an alarm quieter than the others is a bug.

**Gain is the lesser of loudness-target and peak-headroom.** One isolated
transient can eat all the headroom and leave a whole file inaudible. That is not
hypothetical: the autumn woods recording has a single spike at 147.63s sitting
27 dB above the recording's typical peak, which flattened the first attempt to
-37.5 LUFS. The window now starts at 165s to step around it.

**Vorbis, not Opus.** `minSdk` is 26 and Android only reads Opus-in-`.ogg` from
API 29. Vorbis-in-`.ogg` has worked since API 1. Encoded with `oggenc` rather
than ffmpeg's built-in vorbis encoder, which is markedly worse.

Output is 44100 Hz stereo, matching `SleepAudioService.SAMPLE_RATE`, so nothing
resamples at runtime.

## Licence provenance

Every file below is from freesound.org. **This table is the compliance record.**
Anything marked CC BY needs a visible credit in the app before release.

| Prepared file | Source | Author | Licence | Credit needed |
| --- | --- | --- | --- | --- |
| `sleep_ocean_2` | [827529](https://freesound.org/s/827529/) | YevgVerh | CC0 | no |
| `sleep_ocean_3` | [827530](https://freesound.org/s/827530/) | YevgVerh | CC0 | no |
| `sleep_woods` | [832629](https://freesound.org/s/832629/) | kvgarlic | CC0 | no |
| `sleep_wind` | [117136](https://freesound.org/s/117136/) | cobratronik | CC0 | no |
| `wake_clock_bell` | [108249](https://freesound.org/s/108249/) | Nick-Nac | CC0 | no |
| `wake_birds_1` | [345852](https://freesound.org/s/345852/) | hargissssound | CC0 | no |
| `wake_birds_2` | [387978](https://freesound.org/s/387978/) | DCPoke | CC0 | no |
| `sleep_rain` | [243629](https://freesound.org/s/243629/) | lebaston100 | **CC BY 4.0** | **yes** |
| `wake_chimes` | [848198](https://freesound.org/s/848198/) | sonocrea | **CC BY 4.0** | **yes** |

### Rejected

`263995` "Crashing Ocean Waves (3 hours)" by hansendex is **CC BY-NC 4.0**. The
NonCommercial clause is a bad fit for anything that might reach a store listing,
and the two CC0 ocean recordings above already cover the same ground. It was
prepared, measured, and then deleted. Do not re-add it.

## Measured output

Verified after encoding, not assumed. Seam ratio compares the sample step across
the loop point against the typical adjacent-sample step in the same material;
under about 6x is continuous.

| File | Length | Loudness | True peak | Size | Seam |
| --- | --- | --- | --- | --- | --- |
| `sleep_rain` | 60s | -20.06 LUFS | -6.14 dBTP | 500K | 3.4x |
| `sleep_ocean_2` | 75s | -20.22 LUFS | -2.05 dBTP | 716K | 1.8x |
| `sleep_ocean_3` | 90s | -20.67 LUFS | -5.25 dBTP | 860K | 6.7x, needs an ear check |
| `sleep_woods` | 120s | -20.31 LUFS | -1.85 dBTP | 1.1M | 1.0x |
| `sleep_wind` | 45s | -20.10 LUFS | -5.62 dBTP | 356K | 0.5x |
| `wake_clock_bell` | 8.43s | -14.54 LUFS | -3.74 dBTP | 76K | 2.5x |
| `wake_chimes` | 15s | -13.41 LUFS | -4.38 dBTP | 176K | 1.5x |
| `wake_birds_1` | 90s | -16.38 LUFS | -1.51 dBTP | 1.2M | 1.8x |
| `wake_birds_2` | 20s | -19.29 LUFS | -2.19 dBTP | 264K | 2.0x |

Total 5.2 MB. Sleep loops sit at -20 LUFS and wake tones at -14, so switching
between sounds does not change perceived level and no alarm is louder than
another. `wake_birds_2` is the one outlier at -19.3: its source is very
sparse and quiet, and pushing it further would have raised its noise floor.

Loop lengths are not uniform on purpose. Rain and wind are stationary noise, so
a short loop is undetectable. Surf and birdsong have rhythm the ear tracks, so
they need 75 to 120 seconds before the pattern stops announcing itself.

## Wired in

Decided and shipped in v0.2.2. `NoiseGen` keeps the eight noise-shaped voices,
where synthesis wins outright, and these five recordings cover what it cannot
fake. The Sounds panel renders both as one list of thirteen; `SleepAudioService`
is where they diverge.

The four wake tones are also live. Before v0.2.2 each alarm stored one of six
tone names and every one of them played the device default, so the picker was
decorative. `AlarmTone.SYSTEM` keeps that old behaviour available on purpose,
and is the fallback if a bundled tone ever fails to open.

Files are copied to `app/src/main/res/raw/`. They are stored rather than
deflated in the APK, which is what lets `LoopPlayer` read them straight from the
package. Re-run `prep_sounds.sh` and copy again if you change a loop.

The two CC BY credits are rendered at the foot of the Sounds panel, from
`AUDIO_CREDITS`. If you add a recording under a licence that needs attribution,
put the credit in the enum and it appears there automatically.
