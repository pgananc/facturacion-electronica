#!/bin/bash

# Script to run ebill-product microservice
# Usage: ./run.sh [profile]
# Example: ./run.sh localhost

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get profile from argument or use default
PROFILE=${1:-localhost}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  eBill Product Microservice Launcher  ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed. Please install Java 17 or higher.${NC}"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java 17 or higher is required. Current version: $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java version: $(java -version 2>&1 | head -n 1)${NC}"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed. Please install Maven.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Maven version: $(mvn -version | head -n 1)${NC}"
echo ""

# Check if PostgreSQL is running
echo -e "${YELLOW}🔍 Checking PostgreSQL connection...${NC}"
if ! nc -z localhost 5433 2>/dev/null; then
    echo -e "${YELLOW}⚠️  PostgreSQL is not running on port 5433${NC}"
    echo -e "${YELLOW}   You can start it with: docker-compose up -d${NC}"
    echo ""
    read -p "Do you want to start PostgreSQL with Docker? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if command -v docker-compose &> /dev/null; then
            echo -e "${BLUE}🐳 Starting PostgreSQL...${NC}"
            docker-compose up -d
            echo -e "${GREEN}✓ PostgreSQL started${NC}"
            sleep 5
        else
            echo -e "${RED}❌ docker-compose is not installed${NC}"
            exit 1
        fi
    fi
fi

echo ""
echo -e "${BLUE}🚀 Starting ebill-product with profile: ${YELLOW}${PROFILE}${NC}"
echo -e "${BLUE}📡 Application will be available at: ${YELLOW}http://localhost:8380${NC}"
echo ""

# Configure JAVA_HOME to use Java 17
if [ -d "/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home" ]; then
    export JAVA_HOME="/Users/jguerreroh/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo -e "${GREEN}✓ Using Java 17 (Corretto)${NC}"
elif [ -d "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" ]; then
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo -e "${GREEN}✓ Using Java 17 (Temurin)${NC}"
fi

echo -e "${YELLOW}Press Ctrl+C to stop the application${NC}"
echo ""

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=${PROFILE}

