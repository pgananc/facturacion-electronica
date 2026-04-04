#!/bin/bash

# Script para detener todos los procesos de Flutter

echo "🛑 Deteniendo procesos de Flutter..."

pkill -f "flutter run" 2>/dev/null || true
pkill -f "dartvm" 2>/dev/null || true
pkill -f "dartaotruntime" 2>/dev/null || true
pkill -f "development-service" 2>/dev/null || true

sleep 2

echo "✅ Procesos detenidos"
echo ""
echo "Verificando procesos restantes..."
ps aux | grep -i "flutter\|dart" | grep -v grep | grep -v "detener_flutter" || echo "✅ No hay procesos de Flutter corriendo"

