#!/bin/bash

# Script para ejecutar la app con todas las configuraciones correctas

echo "🛑 Deteniendo procesos anteriores de Flutter..."
pkill -f "flutter run" 2>/dev/null || true
pkill -f "dartvm" 2>/dev/null || true
sleep 1

# IP y dispositivo conocidos
IP="192.168.100.227"
DEVICE="00008140-00182C520062201C"

echo "🚀 Ejecutando eBill Mobile"
echo "📱 Dispositivo: Jonathan's iPhone"
echo "🌐 IP Backend: $IP"
echo ""

# Obtener dependencias
echo "📦 Obteniendo dependencias..."
flutter pub get 2>&1 | tail -3

echo ""
echo "📱 Iniciando Flutter..."
echo ""

# Ejecutar con todas las configuraciones correctas
flutter run -d "$DEVICE" \
  --dart-define=API_BASE_URL=http://$IP:8280 \
  --dart-define=API_AUTH_URL=http://$IP:8280/api \
  --dart-define=API_CUSTOMER_URL=http://$IP:8180/api/client \
  --dart-define=API_PRODUCT_URL=http://$IP:8380/api/product \
  --dart-define=API_BILLING_URL=http://$IP:8480/api/invoice

