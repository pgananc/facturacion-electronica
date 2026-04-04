#!/usr/bin/env bash
# Build standalone release apuntando a producción (sin modo debug).
# Salida típica:
#   build/app/outputs/flutter-apk/app-release.apk
#   build/app/outputs/bundle/release/app-release.aab
set -euo pipefail
cd "$(dirname "$0")/.."

DEFINES=(--dart-define=ENV=prod)

case "${1:-apk}" in
  apk)
    flutter build apk --release "${DEFINES[@]}"
    echo "APK: build/app/outputs/flutter-apk/app-release.apk"
    ;;
  aab)
    flutter build appbundle --release "${DEFINES[@]}"
    echo "AAB: build/app/outputs/bundle/release/app-release.aab"
    ;;
  ios)
    flutter build ios --release --no-codesign "${DEFINES[@]}"
    echo "Sin firmar: build/ios/iphoneos/Runner.app (luego archive en Xcode para App Store / Ad Hoc)"
    ;;
  ipa)
    flutter build ipa --release "${DEFINES[@]}"
    echo "IPA (requiere firma en Xcode/cuenta Apple configurada)"
    ;;
  *)
    echo "Uso: $0 [apk|aab|ios|ipa]" >&2
    exit 1
    ;;
esac
