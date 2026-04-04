#!/bin/bash

# Script para configurar Java 17 permanentemente en tu sistema
# Usage: ./setup-java17.sh

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Configurar Java 17 Permanentemente   ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Detectar Java 17
JAVA_17_HOME=""
if [ -d "/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home" ]; then
    JAVA_17_HOME="/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home"
    echo -e "${GREEN}✓ Encontrado: Amazon Corretto 17.0.16${NC}"
elif [ -d "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" ]; then
    JAVA_17_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
    echo -e "${GREEN}✓ Encontrado: OpenJDK 17 (Temurin)${NC}"
else
    echo -e "${RED}❌ No se encontró Java 17 instalado${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Este script agregará las siguientes líneas a tu ~/.zshrc:${NC}"
echo ""
echo -e "export JAVA_HOME=\"$JAVA_17_HOME\""
echo -e "export PATH=\"\$JAVA_HOME/bin:\$PATH\""
echo ""

read -p "¿Deseas continuar? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Cancelado${NC}"
    exit 0
fi

# Backup del .zshrc
if [ -f ~/.zshrc ]; then
    cp ~/.zshrc ~/.zshrc.backup.$(date +%Y%m%d_%H%M%S)
    echo -e "${GREEN}✓ Backup creado: ~/.zshrc.backup$(NC}"
fi

# Agregar configuración al .zshrc
echo "" >> ~/.zshrc
echo "# Java 17 Configuration (added by setup-java17.sh)" >> ~/.zshrc
echo "export JAVA_HOME=\"$JAVA_17_HOME\"" >> ~/.zshrc
echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >> ~/.zshrc

echo ""
echo -e "${GREEN}✓ Configuración agregada a ~/.zshrc${NC}"
echo ""
echo -e "${YELLOW}Para aplicar los cambios:${NC}"
echo -e "  ${BLUE}source ~/.zshrc${NC}"
echo ""
echo -e "${YELLOW}O cierra y abre una nueva terminal${NC}"
echo ""

