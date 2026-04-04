#!/bin/bash

# Script ultra-simplificado - sin detección automática
# Usa directamente el dispositivo USB

IP="192.168.100.227"
DEVICE="00008140-00182C520062201C"

echo "🚀 Ejecutando app"
echo "📱 Dispositivo: $DEVICE"
echo "🌐 IP: $IP"
echo ""

flutter pub get

flutter run -d "$DEVICE" \
  --dart-define=API_BASE_URL=http://$IP:8280 \
  --dart-define=API_AUTH_URL=http://$IP:8280/api/auth \
  --dart-define=API_CUSTOMER_URL=http://$IP:8180/api/customers \
  --dart-define=API_PRODUCT_URL=http://$IP:8380/api/products \
  --dart-define=API_BILLING_URL=http://$IP:8480/api/billing

