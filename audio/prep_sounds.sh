#!/usr/bin/env bash
# Turns raw Freesound downloads into seamless, level-matched OGG Vorbis loops.
#
# Three decisions worth knowing:
#
#  1. Crossfade-wrap, not a butt splice. The tail is folded back onto the head
#     with equal-power (qsin) fades. A linear fade would dip in level at the
#     join on uncorrelated material, which is exactly the sort of thing that
#     surfaces you from light sleep.
#
#  2. Static gain, not dynamic normalisation. loudnorm is measured in a first
#     pass and applied as a fixed dB offset. A dynamic normaliser drifts across
#     the seam and makes the join audible.
#
#  3. Vorbis, not Opus. minSdk is 26 and Android only reads Opus-in-.ogg from
#     API 29. Vorbis-in-.ogg has worked since API 1. Encoded with oggenc rather
#     than ffmpeg's built-in vorbis encoder, which is markedly worse.
#
# Output is 44100 Hz stereo, matching SleepAudioService.SAMPLE_RATE.

set -euo pipefail
SRC="$HOME/Downloads"
OUT="$HOME/Documents/GIT/AlarmHub/audio/prepared"
TMP="${TMPDIR:-/tmp}/alarmhub_prep"
mkdir -p "$OUT" "$TMP"
trap 'rm -rf "$TMP"' EXIT

# measure <file> <start> <dur> -> "input_i input_tp"
measure() {
  local in="$1" ss="$2" d="$3" j
  j=$(ffmpeg -hide_banner -ss "$ss" -t "$d" -i "$in" \
        -af loudnorm=I=-20:TP=-2.0:print_format=json -f null - 2>&1)
  echo "$(echo "$j" | awk -F'"' '/"input_i"/{print $4}' | tail -1)" \
       "$(echo "$j" | awk -F'"' '/"input_tp"/{print $4}' | tail -1)"
}

# Gain is the LESSER of what the loudness target asks for and what the peak
# headroom allows. Sleep loops get no limiter at all: a limiter is dynamic, and
# anything dynamic drifts across the crossfade seam. Wake tones may exceed the
# peak-safe gain by up to LIMIT_HEADROOM dB, with alimiter catching the
# transients, because an alarm that is quieter than the others is a bug.
CEILING_TP=-2.0
LIMIT_HEADROOM=6.0

pick_gain() {  # <input_i> <input_tp> <target_lufs> <extra_db>
  awk -v i="$1" -v tp="$2" -v t="$3" -v x="$4" -v c="$CEILING_TP" 'BEGIN{
    gl = t - i; gp = c - tp + x;
    g = (gl < gp) ? gl : gp;
    printf "%.2f", g
  }'
}

# wrap <infile> <outname> <start> <loop_sec> <overlap_sec> <target_lufs> <ogg_q> <kind>
wrap() {
  local in="$SRC/$1" name="$2" ss="$3" loop="$4" ov="$5" target="$6" q="$7" kind="${8:-sleep}"
  local need m ii itp gain post extra
  need=$(awk -v l="$loop" -v o="$ov" 'BEGIN{printf "%.3f", l+o}')
  m=$(measure "$in" "$ss" "$need"); ii=${m%% *}; itp=${m##* }
  case "$ii" in ''|*inf*) ii=-23;; esac
  case "$itp" in ''|*inf*) itp=-6;; esac

  if [ "$kind" = wake ]; then
    extra="$LIMIT_HEADROOM"
    post=",alimiter=limit=0.794:attack=5:release=60:level=disabled"
  else
    extra=0
    post=""
  fi
  gain=$(pick_gain "$ii" "$itp" "$target" "$extra")

  ffmpeg -hide_banner -loglevel error -y -ss "$ss" -t "$need" -i "$in" \
    -filter_complex "\
[0:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo,volume=${gain}dB${post}[s];\
[s]asplit=2[s1][s2];\
[s1]atrim=0:${loop},asetpts=N/SR/TB[main];\
[s2]atrim=${loop}:${need},asetpts=N/SR/TB,afade=t=out:st=0:d=${ov}:curve=qsin[tail];\
[main]asplit=2[m1][m2];\
[m1]atrim=0:${ov},asetpts=N/SR/TB,afade=t=in:st=0:d=${ov}:curve=qsin[head];\
[m2]atrim=${ov}:${loop},asetpts=N/SR/TB[body];\
[head][tail]amix=inputs=2:normalize=0[seam];\
[seam][body]concat=n=2:v=0:a=1[out]" \
    -map "[out]" -ar 44100 -ac 2 -c:a pcm_s16le "$TMP/$name.wav"
  oggenc -Q -q "$q" -o "$OUT/$name.ogg" "$TMP/$name.wav"
  rm -f "$TMP/$name.wav"
  printf "  %-22s loop=%-7s ovl=%-7s src=%-7s gain=%-7s %s\n" \
    "$name" "${loop}s" "${ov}s" "${ii}" "${gain}dB" "$(du -h "$OUT/$name.ogg" | cut -f1)"
}

echo "=== SLEEP LOOPS  (-20 LUFS, vorbis q1) ==="
wrap "243629__lebaston100__heavy-rain.wav"                          sleep_rain            15   60    6  -20  1
wrap "827529__yevgverh__ocean_coast_03_092025_0659am.wav"           sleep_ocean_2      10   75    8  -20  1
wrap "827530__yevgverh__ocean_coast_04_092025_0659am.wav"           sleep_ocean_3      20   90    8  -20  1
# 263995 "Crashing Ocean Waves (3 hours)" by hansendex is CC BY-NC 4.0. The
# NonCommercial clause is a bad fit for a store listing and the two CC0 ocean
# recordings already cover it. Deliberately not built. See README.md.
# Window starts at 165s: there is an isolated transient at 147.63s, 27 dB above
# the recording's typical peak, and any window containing it loses all headroom.
wrap "832629__kvgarlic__autumn-woods-soundscape-mix-two-november-7-2025.wav" \
                                                                    sleep_woods           165  120   10  -20  1
wrap "117136__cobratronik__wind__artic__cold.wav"                   sleep_wind             3   45    5  -20  1

echo
echo "=== WAKE TONES  (-14 LUFS, vorbis q4) ==="
# The clock bell strikes every 2.107s. An overlap of exactly one strike period
# folds strike 5 onto strike 1, attack on attack, so four strikes loop as one
# unbroken bell tower rather than a phrase with a join in it.
wrap "108249__nick-nac__clock-bell-chimes.wav"                      wake_clock_bell     0.92 8.428 2.107 -14 4
wrap "848198__sonocrea__tubular-chimes.wav"                         wake_chimes    2   15    3  -14  4
wrap "345852__hargissssound__spring-birds-loop-with-low-cut-new-jersey.wav" \
                                                                    wake_birds_1       10   90    8  -16  3  wake
wrap "387978__dcpoke__birds-singing-03.wav"                         wake_birds_2       1   20    3  -16  3

echo
echo "=== TOTAL ==="
du -ch "$OUT"/*.ogg | tail -1
