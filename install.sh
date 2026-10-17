#!/usr/bin/env bash
# Installs Not in Front of the Kids on a TV over Wi-Fi with adb and turns its
# accessibility service on, keeping any other enabled services (e.g. Projectivy).
#
# Usage: ./install.sh <tv-ip> [path/to/not-in-front-of-the-kids.apk]
#
# Before running: on the TV enable Developer options and USB debugging
# (see README), then approve the "Allow USB debugging?" prompt on the TV.
set -euo pipefail
cd "$(dirname "$0")"

IP=${1:?Usage: ./install.sh <tv-ip> [apk]}
APK=${2:-build/not-in-front-of-the-kids.apk}
TV="$IP"; [[ "$TV" == *:* ]] || TV="$IP:5555"
PKG=io.github.stormrdoteth.notinfrontofthekids
SERVICE=$PKG/.ShieldService

[ -f "$APK" ] || { echo "No APK at $APK. Run ./build.sh or download one from Releases." >&2; exit 1; }

adb connect "$TV" >/dev/null
for i in $(seq 1 30); do
  state=$(adb -s "$TV" get-state 2>/dev/null || true)
  [ "$state" = "device" ] && break
  [ "$i" = 1 ] && echo "Waiting for you to press Allow on the TV…"
  sleep 2; adb connect "$TV" >/dev/null 2>&1 || true
done
[ "$(adb -s "$TV" get-state 2>/dev/null)" = "device" ] || { echo "Couldn't connect to $TV." >&2; exit 1; }

echo "• Installing"
adb -s "$TV" install -r "$APK"

# Android 13+ blocks accessibility for sideloaded apps unless this is allowed.
adb -s "$TV" shell appops set "$PKG" ACCESS_RESTRICTED_SETTINGS allow >/dev/null 2>&1 || true

echo "• Turning on the accessibility service"
CUR=$(adb -s "$TV" shell settings get secure enabled_accessibility_services | tr -d '\r')
case "$CUR" in
  *"$SERVICE"*) ;;
  ""|null) adb -s "$TV" shell settings put secure enabled_accessibility_services "$SERVICE" ;;
  *) adb -s "$TV" shell settings put secure enabled_accessibility_services "$CUR:$SERVICE" ;;
esac
adb -s "$TV" shell settings put secure accessibility_enabled 1

echo "• Opening setup on the TV"
adb -s "$TV" shell am start -n "$PKG/.SetupActivity" >/dev/null
echo "✓ Done. On the TV, choose “Shield button” and press the button you want to use."
