#!/bin/bash

# Script para verificar conectividad con todos los microservicios

IP="192.168.100.227"

echo "🔍 Verificando conectividad con microservicios..."
echo ""

# Auth Service (8280)
echo "1️⃣  Auth Service (Puerto 8280):"
if curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://$IP:8280/api/login" | grep -q "405\|200\|401"; then
    echo "   ✅ ACTIVO"
else
    echo "   ❌ NO RESPONDE"
fi

# Customer Service (8180)
echo "2️⃣  Customer Service (Puerto 8180):"
if curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://$IP:8180/api/client/company/1" | grep -q "200\|401\|404"; then
    echo "   ✅ ACTIVO"
else
    echo "   ❌ NO RESPONDE"
fi

# Product Service (8380)
echo "3️⃣  Product Service (Puerto 8380):"
if curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://$IP:8380/api/product/company/1" | grep -q "200\|401\|404"; then
    echo "   ✅ ACTIVO"
else
    echo "   ❌ NO RESPONDE"
fi

# Billing Service (8480)
echo "4️⃣  Billing Service (Puerto 8480):"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://$IP:8480/api/invoice" 2>&1)
if echo "$HTTP_CODE" | grep -q "200\|401\|403"; then
    echo "   ✅ ACTIVO (HTTP $HTTP_CODE)"
elif echo "$HTTP_CODE" | grep -q "000"; then
    echo "   ❌ NO RESPONDE - Servicio caído o puerto incorrecto"
else
    echo "   ⚠️  HTTP $HTTP_CODE"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "💡 Si algún servicio NO RESPONDE:"
echo "   1. Verifica que el microservicio esté corriendo"
echo "   2. Verifica el puerto en el application.properties"
echo "   3. Verifica que no haya firewall bloqueando"
echo ""
