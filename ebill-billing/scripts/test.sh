#!/bin/bash

# Script para ejecutar tests
# eBill Billing - Run Tests

set -e

echo "🧪 Ejecutando tests de eBill Billing..."
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "pom.xml" ]; then
    cd ..
    if [ ! -f "pom.xml" ]; then
        echo "❌ Error: No se encuentra pom.xml"
        exit 1
    fi
fi

# Cargar variables de entorno si existe .env
if [ -f ".env" ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Ejecutar tests
mvn test -Dspring.profiles.active=localhost

echo ""
echo "✅ Tests completados"



