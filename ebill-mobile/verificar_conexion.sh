#!/bin/bash

# Script para verificar la conectividad con el backend

echo "🔍 Verificando conectividad con el backend..."
echo ""

# Obtener IP local
IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1)

if [ -z "$IP" ]; then
    echo "❌ No se pudo detectar la IP local"
    exit 1
fi

echo "📍 IP local detectada: $IP"
echo ""

# Verificar que el backend esté ejecutándose
echo "🔌 Verificando servicios del backend..."
echo ""

# Auth Service (puerto 8280)
echo -n "Auth Service (puerto 8280): "
if curl -s --connect-timeout 3 "http://$IP:8280/api/auth/health" > /dev/null 2>&1 || curl -s --connect-timeout 3 "http://localhost:8280/api/auth/health" > /dev/null 2>&1; then
    echo "✅ OK"
else
    echo "❌ NO RESPONDE"
    echo "   Verifica que el servicio de autenticación esté ejecutándose"
fi

# Customer Service (puerto 8180)
echo -n "Customer Service (puerto 8180): "
if curl -s --connect-timeout 3 "http://$IP:8180/api/customers/health" > /dev/null 2>&1 || curl -s --connect-timeout 3 "http://localhost:8180/api/customers/health" > /dev/null 2>&1; then
    echo "✅ OK"
else
    echo "❌ NO RESPONDE"
    echo "   Verifica que el servicio de clientes esté ejecutándose"
fi

# Product Service (puerto 8380)
echo -n "Product Service (puerto 8380): "
if curl -s --connect-timeout 3 "http://$IP:8380/api/products/health" > /dev/null 2>&1 || curl -s --connect-timeout 3 "http://localhost:8380/api/products/health" > /dev/null 2>&1; then
    echo "✅ OK"
else
    echo "❌ NO RESPONDE"
    echo "   Verifica que el servicio de productos esté ejecutándose"
fi

# Billing Service (puerto 8480)
echo -n "Billing Service (puerto 8480): "
if curl -s --connect-timeout 3 "http://$IP:8480/api/billing/health" > /dev/null 2>&1 || curl -s --connect-timeout 3 "http://localhost:8480/api/billing/health" > /dev/null 2>&1; then
    echo "✅ OK"
else
    echo "❌ NO RESPONDE"
    echo "   Verifica que el servicio de facturación esté ejecutándose"
fi

echo ""
echo "📱 Para probar desde el iPhone:"
echo "   1. Asegúrate de que el iPhone esté en la misma red WiFi"
echo "   2. Abre Safari en el iPhone"
echo "   3. Intenta acceder a: http://$IP:8180/api/auth/health"
echo "   4. Si no funciona, verifica el firewall del Mac:"
echo "      System Settings > Network > Firewall"
echo ""

