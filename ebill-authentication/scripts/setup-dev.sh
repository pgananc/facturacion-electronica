#!/bin/bash

# Script para configurar entorno de desarrollo
# Autor: Wallparisoft Dev Team

set -e

echo "🔧 Configurando entorno de desarrollo para ebill-authentication..."
echo ""

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 1. Verificar herramientas
echo -e "${YELLOW}1️⃣  Verificando herramientas necesarias...${NC}"

command -v java >/dev/null 2>&1 || { echo "❌ Java no encontrado"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven no encontrado"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "❌ Docker no encontrado"; exit 1; }

echo -e "${GREEN}✅ Herramientas verificadas${NC}"
echo ""

# 2. Levantar servicios Docker
echo -e "${YELLOW}2️⃣  Iniciando servicios Docker...${NC}"
docker-compose up -d

echo "   Esperando que PostgreSQL esté listo..."
sleep 5

# Verificar PostgreSQL
until docker exec ebill-auth-postgres pg_isready -U postgres > /dev/null 2>&1; do
    echo "   Esperando PostgreSQL..."
    sleep 2
done

echo -e "${GREEN}✅ PostgreSQL listo${NC}"
echo ""

# 3. Compilar proyecto
echo -e "${YELLOW}3️⃣  Compilando proyecto...${NC}"
mvn clean compile -DskipTests
echo -e "${GREEN}✅ Proyecto compilado${NC}"
echo ""

# 4. Ejecutar migraciones
echo -e "${YELLOW}4️⃣  Ejecutando migraciones Flyway...${NC}"
mvn flyway:migrate -Dspring-boot.run.profiles=localhost
echo -e "${GREEN}✅ Migraciones aplicadas${NC}"
echo ""

# 5. Resumen
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${GREEN}✨ Entorno de desarrollo configurado ✨${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo ""
echo "Servicios disponibles:"
echo "  🐘 PostgreSQL:      localhost:5433"
echo "  🔴 Redis:           localhost:6379"
echo "  💻 Adminer:         http://localhost:8081"
echo "  📊 Redis Commander: http://localhost:8082"
echo ""
echo "Para iniciar el microservicio:"
echo "  ${YELLOW}./scripts/start-local.sh${NC}"
echo ""

