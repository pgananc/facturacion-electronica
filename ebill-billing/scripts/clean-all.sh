#!/bin/bash

# Script para limpiar completamente el proyecto
# eBill Billing - Clean All

set -e

echo "🧹 Limpiando proyecto eBill Billing..."
echo ""

# Limpiar target de Maven
echo "📦 Limpiando target de Maven..."
mvn clean
echo "✅ Maven limpiado"

# Detener y eliminar contenedores
echo ""
echo "🐳 Limpiando contenedores Docker..."
if docker ps -a | grep -q ebill-postgres; then
    docker stop ebill-postgres 2>/dev/null || true
    docker rm ebill-postgres 2>/dev/null || true
fi
if docker ps -a | grep -q ebill-pgadmin; then
    docker stop ebill-pgadmin 2>/dev/null || true
    docker rm ebill-pgadmin 2>/dev/null || true
fi

# Preguntar si eliminar volúmenes
echo ""
read -p "¿Deseas eliminar los volúmenes de Docker (esto borrará los datos de la BD)? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️  Eliminando volúmenes..."
    docker-compose down -v 2>/dev/null || true
    echo "✅ Volúmenes eliminados"
else
    echo "⏭️  Volúmenes conservados"
fi

echo ""
echo "✅ Limpieza completa"



