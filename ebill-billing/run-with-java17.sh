#!/bin/bash

# Script para ejecutar con Java 17
# Configura JAVA_HOME y ejecuta la aplicación

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}🔧 Configurando Java 17...${NC}"

# Configurar JAVA_HOME para Java 17
export JAVA_HOME="/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Verificar versión
echo -e "${YELLOW}☕ Verificando Java:${NC}"
java -version

# Verificar Maven
echo -e "\n${YELLOW}📦 Verificando Maven:${NC}"
mvn -version | head -1

# Ejecutar la aplicación con el perfil localhost
echo -e "\n${GREEN}🚀 Ejecutando eBill Billing (perfil: localhost)...${NC}"
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=localhost

