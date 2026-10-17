#!/usr/bin/env bash
# Builds build/commercial-shield.apk with the plain Android SDK tools (no Gradle).
#
# Needs: JDK 17+, Android SDK with "platforms;android-31" and "build-tools;34.0.0".
#
# Environment overrides:
#   ANDROID_HOME     SDK location (default: ~/Library/Android/sdk on macOS, ~/Android/Sdk on Linux)
#   BUILD_TOOLS      build-tools version (default 34.0.0)
#   PLATFORM         platform to compile against (default android-31)
#   KEYSTORE, KEYSTORE_PASS, KEY_ALIAS, KEY_PASS
#                    release signing key. If unset, a local debug key is created
#                    in ./debug.keystore (git-ignored) and used instead.
set -euo pipefail
cd "$(dirname "$0")"

if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then ANDROID_HOME="$HOME/Library/Android/sdk"; else ANDROID_HOME="$HOME/Android/Sdk"; fi
fi
BT="$ANDROID_HOME/build-tools/${BUILD_TOOLS:-34.0.0}"
JAR="$ANDROID_HOME/platforms/${PLATFORM:-android-31}/android.jar"
for f in "$BT/aapt2" "$BT/d8" "$BT/apksigner" "$JAR"; do
  [ -e "$f" ] || { echo "Missing $f — install it with sdkmanager (see README)." >&2; exit 1; }
done

rm -rf build && mkdir -p build/gen build/classes build/dex

echo "• Compiling resources"
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/unsigned.apk -I "$JAR" --manifest AndroidManifest.xml \
  --java build/gen build/res.zip

echo "• Compiling Java"
javac -source 8 -target 8 -nowarn -Xlint:-options -encoding UTF-8 -bootclasspath "$JAR" \
  -d build/classes $(find src build/gen -name '*.java')
"$BT/d8" --release --min-api 24 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q ../unsigned.apk classes.dex)

echo "• Signing"
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ -z "${KEYSTORE:-}" ]; then
  KEYSTORE=debug.keystore KEYSTORE_PASS=android KEY_ALIAS=debug KEY_PASS=android
  [ -f "$KEYSTORE" ] || keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android \
    -alias debug -dname "CN=Commercial Shield debug" -keyalg RSA -keysize 2048 -validity 10000 >/dev/null
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KEYSTORE_PASS" \
  --ks-key-alias "$KEY_ALIAS" --key-pass "pass:${KEY_PASS:-$KEYSTORE_PASS}" \
  --out build/commercial-shield.apk build/aligned.apk

echo "✓ build/commercial-shield.apk"
shasum -a 256 build/commercial-shield.apk 2>/dev/null || sha256sum build/commercial-shield.apk
