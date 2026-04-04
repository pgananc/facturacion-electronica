#!/bin/bash

# Script para detener el entorno de desarrollo
# eBill Billing - Stop Development Environment

set -e

echo "🛑 Deteniendo entorno de desarrollo de eBill Billing..."
echo ""

# Detener Docker Compose
if docker ps | grep -q ebill-postgres; then
    echo "🐳 Deteniendo servicios Docker..."
    docker-compose down
    echo "✅ Servicios Docker detenidos"
else
    echo "⚠️  No hay servicios Docker corriendo"
fi

echo ""
echo "✅ Entorno de desarrollo detenido"



