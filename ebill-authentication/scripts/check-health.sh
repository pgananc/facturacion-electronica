#!/bin/bash

# Script para verificar el estado del microservicio y sus dependencias
# Autor: Wallparisoft Dev Team

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🏥 Verificando estado de ebill-authentication..."
echo ""

# Función para verificar servicio
check_service() {
    local name=$1
    local url=$2
    
    if curl -s -f "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $name: OK${NC}"
        return 0
    else
        echo -e "${RED}❌ $name: NO DISPONIBLE${NC}"
        return 1
    fi
}

# Función para verificar puerto
check_port() {
    local name=$1
    local host=$2
    local port=$3
    
    if nc -z "$host" "$port" 2>/dev/null; then
        echo -e "${GREEN}✅ $name ($host:$port): OK${NC}"
        return 0
    else
        echo -e "${RED}❌ $name ($host:$port): NO DISPONIBLE${NC}"
        return 1
    fi
}

# Verificar PostgreSQL
check_port "PostgreSQL" "localhost" "5433"

# Verificar Redis
check_port "Redis" "localhost" "6379"

# Verificar microservicio
check_service "Microservicio" "http://localhost:8280/actuator/health"

echo ""

# Verificar proceso Java
if pgrep -f "EBillAuthenticationApplication" > /dev/null; then
    echo -e "${GREEN}✅ Proceso Java: CORRIENDO${NC}"
    PID=$(pgrep -f "EBillAuthenticationApplication")
    echo "   PID: $PID"
else
    echo -e "${YELLOW}⚠️  Proceso Java: NO CORRIENDO${NC}"
fi

echo ""
echo "═══════════════════════════════════════"
echo "URLs útiles:"
echo "  • Adminer:         http://localhost:8081"
echo "  • Redis Commander: http://localhost:8082"
echo "  • Health Check:    http://localhost:8280/actuator/health"
echo "═══════════════════════════════════════"

