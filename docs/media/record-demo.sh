#!/usr/bin/env bash
# Rekam 3 flow Maestro di emulator/HP yang tersambung, ubah jadi GIF untuk README.
# Prasyarat: adb, maestro, ffmpeg di PATH; APK debug sudah terpasang.
# Rekaman dipotong dari langkah pertama sampai terakhir flow (startup Maestro dibuang).
set -euo pipefail
cd "$(dirname "$0")"
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

for name in belanja checkout practicepay; do
  # kecepatan putar per flow (emulator + Maestro lebih lambat dari tangan manusia)
  case $name in belanja) speed=1.5;; checkout) speed=3;; *) speed=2.5;; esac
  maestro test flows/_login.yaml            # reset data + login, tidak direkam
  adb shell screenrecord --size 540x1200 --bit-rate 4000000 /sdcard/$name.mp4 &
  rec=$!; start=$(date +%s.%N); sleep 1
  rm -f /tmp/$name.t0 /tmp/$name.t1
  maestro test -e LOGGED_IN=true flows/$name.yaml 2>&1 | while IFS= read -r line; do
    if [[ ! -f /tmp/$name.t0 && $line == *"Tap on"* ]]; then date +%s.%N > /tmp/$name.t0; fi
    if [[ $line == *COMPLETED* ]]; then date +%s.%N > /tmp/$name.t1; fi
  done
  sleep 1; adb shell pkill -INT screenrecord || true; wait $rec || true; sleep 1
  adb pull /sdcard/$name.mp4 /tmp/$name.mp4 >/dev/null
  ss=$(echo "$(cat /tmp/$name.t0) - $start" | bc -l)
  to=$(echo "$(cat /tmp/$name.t1) - $start + 1.5" | bc -l)
  # 2-pass palet: GIF kecil (lebar 270px, 12 fps), loop tanpa henti
  ffmpeg -loglevel error -y -ss "$ss" -to "$to" -i /tmp/$name.mp4 \
    -vf "setpts=PTS/$speed,fps=12,scale=270:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=96[p];[b][p]paletteuse=dither=bayer:bayer_scale=4" \
    -loop 0 $name.gif
  ls -lh $name.gif
done
