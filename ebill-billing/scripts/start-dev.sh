#!/bin/bash

# Script para iniciar el entorno de desarrollo completo
# eBill Billing - Development Environment Startup

set -e

echo "🚀 Iniciando entorno de desarrollo de eBill Billing..."
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar que estamos en el directorio correcto
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ Error: Este script debe ejecutarse desde la raíz del proyecto${NC}"
    exit 1
fi

# Verificar Java 17
echo "☕ Verificando Java 17..."
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" != "17" ]; then
    echo -e "${RED}❌ Se requiere Java 17, pero se encontró Java $JAVA_VERSION${NC}"
    echo "Por favor, configura JAVA_HOME para apuntar a Java 17"
    exit 1
fi
echo -e "${GREEN}✅ Java 17 encontrado${NC}"

# Verificar Docker
echo ""
echo "🐳 Verificando Docker..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker no está instalado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker encontrado${NC}"

# Verificar archivo .env
echo ""
echo "🔧 Verificando configuración..."
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}⚠️  Archivo .env no encontrado, creándolo desde .env.example...${NC}"
    cp .env.example .env
    echo -e "${GREEN}✅ Archivo .env creado${NC}"
else
    echo -e "${GREEN}✅ Archivo .env encontrado${NC}"
fi

# Iniciar PostgreSQL con Docker Compose
echo ""
echo "🗄️  Iniciando PostgreSQL..."
if docker ps | grep -q ebill-postgres; then
    echo -e "${YELLOW}⚠️  PostgreSQL ya está corriendo${NC}"
else
    docker-compose up -d postgres
    echo "⏳ Esperando a que PostgreSQL esté listo..."
    sleep 5
    echo -e "${GREEN}✅ PostgreSQL iniciado${NC}"
fi

# Verificar conexión a PostgreSQL
echo ""
echo "🔌 Verificando conexión a PostgreSQL..."
until docker exec ebill-postgres pg_isready -U postgres > /dev/null 2>&1; do
    echo "⏳ Esperando conexión a PostgreSQL..."
    sleep 2
done
echo -e "${GREEN}✅ PostgreSQL está listo${NC}"

# Compilar el proyecto
echo ""
echo "🔨 Compilando el proyecto..."
mvn clean install -DskipTests -q
echo -e "${GREEN}✅ Proyecto compilado${NC}"

# Mostrar información
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Entorno de desarrollo listo!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "📊 Base de Datos:"
echo "   - Host: localhost:5433"
echo "   - Database: ebill-dev-bill"
echo "   - Usuario: postgres"
echo "   - Password: postgres"
echo ""
echo "🌐 PgAdmin (opcional):"
echo "   - URL: http://localhost:5050"
echo "   - Email: admin@ebill.com"
echo "   - Password: admin"
echo ""
echo "🚀 Para iniciar la aplicación:"
echo "   - Desde IDE: F5 o Run > Start Debugging"
echo "   - Desde terminal: mvn spring-boot:run -Dspring-boot.run.profiles=localhost"
echo "   - Ejecutar: ./scripts/run-app.sh"
echo ""
echo "📝 La aplicación estará disponible en: http://localhost:8480"
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"



