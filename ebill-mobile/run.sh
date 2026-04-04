#!/bin/bash

# Script ULTRA SIMPLE - Sin detección, sin complicaciones
# Solo ejecuta Flutter con valores conocidos

# Detener procesos anteriores
pkill -f "flutter run" 2>/dev/null
sleep 1

# Valores fijos que sabemos que funcionan
IP="192.168.100.227"
DEVICE="00008140-00182C520062201C"

echo "🚀 Ejecutando app"
echo "📱 Dispositivo: $DEVICE"
echo "🌐 IP: $IP"
echo ""

# Obtener dependencias (sin errores si falla)
flutter pub get 2>&1 | tail -5

# Ejecutar directamente
flutter run -d "$DEVICE" \
  --dart-define=API_BASE_URL=http://$IP:8280 \
  --dart-define=API_AUTH_URL=http://$IP:8280/api \
  --dart-define=API_CUSTOMER_URL=http://$IP:8180/api/client \
  --dart-define=API_PRODUCT_URL=http://$IP:8380/api/product \
  --dart-define=API_BILLING_URL=http://$IP:8480/api/invoice

