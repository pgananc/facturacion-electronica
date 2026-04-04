#!/bin/bash

# Script para cambiar la versión de Java en VSCode/Cursor IDE
# Usage: ./switch-java.sh [version]
# Example: ./switch-java.sh 17

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Java Version Switcher for VSCode     ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Mostrar versiones disponibles
echo -e "${YELLOW}Versiones de Java disponibles en tu sistema:${NC}"
echo ""
/usr/libexec/java_home -V 2>&1 | grep -E "^\s+[0-9]" | nl
echo ""

# Si se proporciona versión como argumento
if [ ! -z "$1" ]; then
    VERSION=$1
else
    # Preguntar por la versión deseada
    echo -e "${YELLOW}¿Qué versión de Java quieres usar? (por ejemplo: 17, 21, 23)${NC}"
    read -p "Versión: " VERSION
fi

# Obtener el path de la versión seleccionada
JAVA_HOME_PATH=$(/usr/libexec/java_home -v $VERSION 2>/dev/null)

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Java $VERSION no está instalado${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Encontrado: $JAVA_HOME_PATH${NC}"
echo ""

# Actualizar settings.json
SETTINGS_FILE=".vscode/settings.json"

if [ ! -f "$SETTINGS_FILE" ]; then
    echo -e "${RED}❌ No se encontró $SETTINGS_FILE${NC}"
    exit 1
fi

# Escapar el path para sed (macOS)
ESCAPED_PATH=$(echo "$JAVA_HOME_PATH" | sed 's/[\/&]/\\&/g')

# Actualizar las 3 ocurrencias en settings.json
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s|\"path\": \".*JavaVirtualMachines/[^\"]*\"|\"path\": \"$ESCAPED_PATH\"|g" "$SETTINGS_FILE"
    sed -i '' "s|\"value\": \".*JavaVirtualMachines/[^\"]*\"|\"value\": \"$ESCAPED_PATH\"|g" "$SETTINGS_FILE"
    sed -i '' "s|\"spring-boot.ls.java.home\": \".*JavaVirtualMachines/[^\"]*\"|\"spring-boot.ls.java.home\": \"$ESCAPED_PATH\"|g" "$SETTINGS_FILE"
else
    # Linux
    sed -i "s|\"path\": \".*JavaVirtualMachines/[^\"]*\"|\"path\": \"$ESCAPED_PATH\"|g" "$SETTINGS_FILE"
    sed -i "s|\"value\": \".*JavaVirtualMachines/[^\"]*\"|\"value\": \"$ESCAPED_PATH\"|g" "$SETTINGS_FILE"
    sed -i "s|\"spring-boot.ls.java.home\": \".*JavaVirtualMachines/[^\"]*\"|\"spring-boot.ls.java.home\": \"$ESCAPED_PATH\"|g" "$SETTINGS_FILE"
fi

echo -e "${GREEN}✓ Configuración actualizada en $SETTINGS_FILE${NC}"
echo ""
echo -e "${BLUE}Versión de Java configurada:${NC}"
$JAVA_HOME_PATH/bin/java -version
echo ""
echo -e "${GREEN}¡Listo! Reinicia VSCode/Cursor IDE para aplicar los cambios${NC}"
echo ""

