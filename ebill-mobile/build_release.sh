#!/bin/bash

# Build eBill Mobile release
# Usage:
#   ./build_release.sh prod apk     → APK de producción
#   ./build_release.sh prod ios     → iOS de producción
#   ./build_release.sh dev apk      → APK de desarrollo

ENV="${1:-prod}"
PLATFORM="${2:-apk}"

echo "🏗️  Build eBill Mobile"
echo "   Entorno:    $ENV"
echo "   Plataforma: $PLATFORM"
echo ""

echo "📦 Obteniendo dependencias..."
flutter pub get 2>&1 | tail -3

DEFINES="--dart-define=ENV=$ENV"

if [ "$ENV" = "dev" ]; then
    IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1)
    IP="${IP:-localhost}"
    DEFINES="$DEFINES --dart-define=DEV_HOST=$IP"
    echo "🔧 Dev host: $IP"
fi

echo ""

case "$PLATFORM" in
    apk)
        echo "📱 Generando APK..."
        flutter build apk --release $DEFINES
        echo ""
        echo "✅ APK generado en: build/app/outputs/flutter-apk/app-release.apk"
        ;;
    appbundle|aab)
        echo "📱 Generando App Bundle..."
        flutter build appbundle --release $DEFINES
        echo ""
        echo "✅ AAB generado en: build/app/outputs/bundle/release/app-release.aab"
        ;;
    ios)
        echo "🍎 Generando build iOS..."
        flutter build ios --release $DEFINES
        echo ""
        echo "✅ Build iOS completado. Abre Xcode para archivar y distribuir."
        ;;
    ipa)
        echo "🍎 Generando IPA..."
        flutter build ipa --release $DEFINES
        echo ""
        echo "✅ IPA generado en: build/ios/ipa/"
        ;;
    *)
        echo "❌ Plataforma no reconocida: $PLATFORM"
        echo "   Usa: apk, appbundle, ios, ipa"
        exit 1
        ;;
esac
