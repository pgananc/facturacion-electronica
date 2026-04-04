#!/bin/bash

# Script para detener el microservicio
# Autor: Wallparisoft Dev Team
# Fecha: Oct 2025

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🛑 Deteniendo ebill-authentication...${NC}"
echo ""

# Verificar si hay proceso en puerto 8280
if lsof -ti:8280 > /dev/null 2>&1; then
    PID=$(lsof -ti:8280)
    echo -e "${YELLOW}Proceso encontrado (PID: $PID)${NC}"
    
    # Matar proceso
    kill -9 $PID
    sleep 2
    
    # Verificar
    if ! lsof -ti:8280 > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Microservicio detenido exitosamente${NC}"
        echo -e "${GREEN}✅ Puerto 8280 liberado${NC}"
    else
        echo -e "${RED}❌ Error: No se pudo detener el proceso${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ No hay proceso corriendo en puerto 8280${NC}"
fi

echo ""
echo -e "${GREEN}Puedes iniciar nuevamente con:${NC}"
echo "  • ./scripts/start-local.sh"
echo "  • Cursor IDE (F5)"
echo ""

