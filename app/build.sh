#!/bin/bash
# Baut die APK ohne Gradle: javac -> d8 -> aapt2 -> zipalign -> apksigner
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$HOME/Android}
export JAVA_HOME=${JAVA_HOME:-$SDK/jdk}
export PATH="$JAVA_HOME/bin:$PATH"
BT=$SDK/build-tools/34.0.0
JAR=$SDK/platforms/android-34/android.jar
OUT=build/apk
rm -rf $OUT && mkdir -p $OUT/classes $OUT/dex

$JAVA_HOME/bin/javac -encoding UTF-8 --release 11 -classpath "$JAR" -d $OUT/classes \
  $(find src -name '*.java') 2>&1 | grep -v '^Note:' || true
# --release 11 erlaubt kein android.jar auf dem bootclasspath -> Fallback
if [ ! -f $OUT/classes/dabs/xcsoar/MainActivity.class ]; then
  $JAVA_HOME/bin/javac -encoding UTF-8 -source 11 -target 11 -bootclasspath "$JAR" -d $OUT/classes \
    $(find src -name '*.java') 2>&1 | grep -v -e '^Note:' -e 'warning: \[options\]' -e '^1 warning' || true
fi
test -f $OUT/classes/dabs/xcsoar/MainActivity.class

$BT/d8 --min-api 26 --lib "$JAR" --output $OUT/dex $(find $OUT/classes -name '*.class')
$BT/aapt2 compile --dir res -o $OUT/res.zip
$BT/aapt2 link -o $OUT/unsigned.apk -I "$JAR" --manifest AndroidManifest.xml $OUT/res.zip
(cd $OUT/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f 4 $OUT/unsigned.apk $OUT/aligned.apk

# Signatur: Release-Schlüssel aus ~/.android/dabs-release.properties (liegt NICHT im Repo).
# Ohne diese Datei wird ein lokaler Debug-Schlüssel erzeugt (APK nur für eigene Tests).
PROPS=${DABS_SIGNING:-$HOME/.android/dabs-release.properties}
if [ -f "$PROPS" ]; then
  KS=$(grep '^KEYSTORE=' "$PROPS" | cut -d= -f2-)
  ALIAS=$(grep '^KEY_ALIAS=' "$PROPS" | cut -d= -f2-)
  PASS=$(grep '^KEYSTORE_PASS=' "$PROPS" | cut -d= -f2-)
else
  KS=$HOME/.android/dabs-debug.keystore; ALIAS=dabs; PASS=android
  if [ ! -f "$KS" ]; then
    mkdir -p "$(dirname "$KS")"
    $JAVA_HOME/bin/keytool -genkeypair -keystore "$KS" -storepass "$PASS" -keypass "$PASS" \
      -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=DABS XCSoar debug" >/dev/null
  fi
  echo "Hinweis: Debug-Signatur ($KS)"
fi
VERSION=$(grep -o 'android:versionName="[^"]*"' AndroidManifest.xml | cut -d'"' -f2)
APK=build/dabs-xcsoar-$VERSION.apk
$BT/apksigner sign --ks "$KS" --ks-key-alias "$ALIAS" --ks-pass "pass:$PASS" --key-pass "pass:$PASS" \
  --out "$APK" $OUT/aligned.apk
cp "$APK" build/dabs-xcsoar.apk
echo "OK: $(pwd)/$APK ($(du -h "$APK" | cut -f1))"
