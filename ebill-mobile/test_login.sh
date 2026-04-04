#!/bin/bash

# Script para probar el endpoint de login

IP="192.168.100.227"
PORT="8280"
ENDPOINT="/api/login"

echo "🧪 Probando login en: http://$IP:$PORT$ENDPOINT"
echo ""

# Probar con username (como en el código Flutter)
echo "📤 Request con 'username':"
curl -X POST "http://$IP:$PORT$ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "tu_usuario",
    "password": "tu_password"
  }' \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -v

echo ""
echo "---"
echo ""

# Probar con userName (si el backend lo espera)
echo "📤 Request con 'userName':"
curl -X POST "http://$IP:$PORT$ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "tu_usuario",
    "password": "tu_password"
  }' \
  -w "\n\nHTTP Status: %{http_code}\n" \
  -v

echo ""
echo "💡 Reemplaza 'tu_usuario' y 'tu_password' con credenciales reales"

