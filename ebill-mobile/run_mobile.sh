#!/bin/bash

# Run eBill Mobile on a connected device
# Usage:
#   ./run_mobile.sh          → dev (auto-detect IP for physical device)
#   ./run_mobile.sh prod     → production

ENV="${1:-dev}"

echo "🛑 Deteniendo procesos de Flutter anteriores..."
pkill -f "flutter run" 2>/dev/null || true
sleep 1

echo "📦 Obteniendo dependencias..."
flutter pub get 2>&1 | tail -3

# Detect connected device (prefer wired, fallback to wireless)
echo "🔍 Buscando dispositivo..."
DEVICE_OUTPUT=$(flutter devices 2>&1 || echo "")
DEVICE_LINE=$(echo "$DEVICE_OUTPUT" | grep -i "iphone\|android" | grep -i "mobile" | grep -v -i "wireless" | head -1)

if [ -z "$DEVICE_LINE" ]; then
    DEVICE_LINE=$(echo "$DEVICE_OUTPUT" | grep -i "iphone\|android" | grep -i "mobile" | head -1)
fi

DEVICE=$(echo "$DEVICE_LINE" | awk -F'•' '{print $2}' | xargs)

DEVICE_FLAG=""
if [ -n "$DEVICE" ]; then
    DEVICE_FLAG="-d $DEVICE"
    echo "📱 Dispositivo: $DEVICE"
else
    echo "⚠️  No se detectó dispositivo, Flutter elegirá uno"
fi

DEFINES="--dart-define=ENV=$ENV"

if [ "$ENV" = "dev" ]; then
    IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1)
    IP="${IP:-localhost}"
    DEFINES="$DEFINES --dart-define=DEV_HOST=$IP"
    echo "🔧 Entorno: DEV → http://$IP"
else
    echo "🚀 Entorno: PROD → *.wallparisoft.com"
fi

echo ""
flutter run $DEVICE_FLAG $DEFINES
