#!/bin/bash

# Script para verificar que el entorno esté correctamente configurado
# Usage: ./check-setup.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  eBill Product - Setup Checker        ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to check command
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 está instalado"
        if [ ! -z "$2" ]; then
            echo -e "  Versión: $($1 $2 2>&1 | head -n 1)"
        fi
        return 0
    else
        echo -e "${RED}✗${NC} $1 NO está instalado"
        return 1
    fi
}

# Function to check file
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        return 0
    else
        echo -e "${RED}✗${NC} $2"
        return 1
    fi
}

# Function to check directory
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        return 0
    else
        echo -e "${RED}✗${NC} $2"
        return 1
    fi
}

ERRORS=0

# Check Java
echo -e "${YELLOW}📦 Verificando herramientas...${NC}"
check_command java "-version" || ((ERRORS++))

# Check Maven
check_command mvn "-version" || ((ERRORS++))

# Check Docker
check_command docker "--version" || ((ERRORS++))

# Check Docker Compose
check_command docker-compose "--version" || ((ERRORS++))

echo ""

# Check Java version
echo -e "${YELLOW}☕ Verificando versión de Java...${NC}"
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ge 17 ]; then
    echo -e "${GREEN}✓${NC} Java 17+ detectado (versión: $JAVA_VERSION)"
else
    echo -e "${RED}✗${NC} Se requiere Java 17 o superior (actual: $JAVA_VERSION)"
    ((ERRORS++))
fi

echo ""

# Check project files
echo -e "${YELLOW}📁 Verificando archivos del proyecto...${NC}"
check_file "pom.xml" "pom.xml" || ((ERRORS++))
check_file "docker-compose.yml" "docker-compose.yml" || ((ERRORS++))
check_file "run.sh" "run.sh" || ((ERRORS++))
check_file "Makefile" "Makefile" || ((ERRORS++))
check_file "src/main/resources/application-localhost.properties" "application-localhost.properties" || ((ERRORS++))

echo ""

# Check IDE configuration
echo -e "${YELLOW}💻 Verificando configuración de IDE...${NC}"
check_dir ".vscode" "Directorio .vscode" || ((ERRORS++))
check_file ".vscode/launch.json" "launch.json (configuración de ejecución)" || ((ERRORS++))
check_file ".vscode/settings.json" "settings.json (configuración del workspace)" || ((ERRORS++))
check_file ".vscode/tasks.json" "tasks.json (tareas de Maven)" || ((ERRORS++))
check_file ".vscode/extensions.json" "extensions.json (extensiones recomendadas)" || ((ERRORS++))

echo ""

# Check Docker
echo -e "${YELLOW}🐳 Verificando Docker...${NC}"
if docker ps &> /dev/null; then
    echo -e "${GREEN}✓${NC} Docker está corriendo"
    
    # Check if postgres container is running
    if docker ps | grep -q ebill-postgres-dev; then
        echo -e "${GREEN}✓${NC} Contenedor PostgreSQL está corriendo"
        
        # Check postgres port
        if nc -z localhost 5433 2>/dev/null; then
            echo -e "${GREEN}✓${NC} PostgreSQL responde en puerto 5433"
        else
            echo -e "${YELLOW}⚠${NC}  PostgreSQL no responde en puerto 5433"
        fi
    else
        echo -e "${YELLOW}⚠${NC}  Contenedor PostgreSQL no está corriendo"
        echo -e "  Ejecuta: ${BLUE}docker-compose up -d${NC}"
    fi
else
    echo -e "${RED}✗${NC} Docker no está corriendo o no tienes permisos"
    ((ERRORS++))
fi

echo ""

# Check if project is compiled
echo -e "${YELLOW}🔨 Verificando compilación del proyecto...${NC}"
if [ -d "target/classes" ]; then
    echo -e "${GREEN}✓${NC} Proyecto compilado"
else
    echo -e "${YELLOW}⚠${NC}  Proyecto no compilado"
    echo -e "  Ejecuta: ${BLUE}mvn clean install${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ ¡Todo listo! El entorno está correctamente configurado${NC}"
    echo ""
    echo -e "Para iniciar la aplicación:"
    echo -e "  ${BLUE}./run.sh localhost${NC}"
    echo -e "  o"
    echo -e "  ${BLUE}make quick-start${NC}"
    echo -e "  o"
    echo -e "  ${BLUE}Presiona F5 en Cursor IDE${NC}"
    echo ""
    echo -e "La aplicación estará disponible en: ${YELLOW}http://localhost:8380${NC}"
else
    echo -e "${RED}⚠️  Se encontraron $ERRORS problemas${NC}"
    echo ""
    echo -e "Revisa los errores anteriores y sigue las instrucciones para corregirlos."
    echo -e "Para más información, consulta: ${BLUE}README.md${NC}"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"

