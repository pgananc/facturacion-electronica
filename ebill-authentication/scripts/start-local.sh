#!/bin/bash

# Script para iniciar el microservicio localmente
# Autor: Wallparisoft Dev Team
# Fecha: Oct 2025

set -e

echo "🚀 Iniciando ebill-authentication en modo local..."
echo ""

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configurar Java 17
echo -e "${YELLOW}📦 Configurando Java 17...${NC}"

# Intentar encontrar Java 17
JAVA_17_HOME="/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home"

if [ -d "$JAVA_17_HOME" ]; then
    export JAVA_HOME="$JAVA_17_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo -e "${GREEN}✅ Java 17 configurado: $JAVA_17_HOME${NC}"
else
    # Intentar detectar automáticamente
    JAVA_17_AUTO=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "")
    if [ -n "$JAVA_17_AUTO" ]; then
        export JAVA_HOME="$JAVA_17_AUTO"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo -e "${GREEN}✅ Java 17 detectado automáticamente: $JAVA_17_AUTO${NC}"
    else
        echo -e "${RED}❌ Error: No se encontró Java 17${NC}"
        echo "   Instala Java 17 o configura JAVA_HOME manualmente"
        exit 1
    fi
fi

# Verificar versión
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo -e "${GREEN}   Versión activa: Java $JAVA_VERSION${NC}"
echo ""

# Verificar PostgreSQL
echo -e "${YELLOW}🐘 Verificando PostgreSQL...${NC}"
if ! nc -z localhost 5433 2>/dev/null; then
    echo -e "${RED}❌ PostgreSQL no está corriendo en puerto 5433${NC}"
    echo "   Ejecuta: docker-compose up -d postgres"
    exit 1
fi
echo -e "${GREEN}✅ PostgreSQL disponible${NC}"
echo ""

# Compilar
echo -e "${YELLOW}🔨 Compilando proyecto...${NC}"
mvn clean compile -DskipTests
echo -e "${GREEN}✅ Compilación exitosa${NC}"
echo ""

# Ejecutar
echo -e "${YELLOW}▶️  Iniciando microservicio...${NC}"
echo ""
mvn spring-boot:run -Dspring-boot.run.profiles=localhost

