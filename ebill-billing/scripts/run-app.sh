#!/bin/bash

# Script para ejecutar la aplicación con el perfil localhost
# eBill Billing - Run Application

set -e

echo "🚀 Iniciando eBill Billing (perfil: localhost)..."
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

# Ejecutar la aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=localhost



