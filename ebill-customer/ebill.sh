#!/bin/bash

# eBill Customer - Script de Utilidades
# Facilita tareas comunes del proyecto

set -e

# Configurar Java 17 automáticamente
if [ -x "/usr/libexec/java_home" ]; then
    # macOS
    JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "")
    if [ -n "$JAVA_17_HOME" ]; then
        export JAVA_HOME="$JAVA_17_HOME"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funciones de ayuda
print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Función para verificar prerequisitos
check_prerequisites() {
    print_header "Verificando prerequisitos..."
    
    # Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        JAVA_MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d'.' -f1)
        
        if [ "$JAVA_MAJOR_VERSION" = "17" ]; then
            print_success "Java $JAVA_VERSION encontrado (✓ Java 17 requerido)"
        else
            print_warning "Java $JAVA_VERSION encontrado (⚠ Java 17 requerido)"
            echo "  El proyecto requiere Java 17. Versión actual: $JAVA_VERSION"
            echo "  El script intentó configurar Java 17 automáticamente."
            if [ -x "/usr/libexec/java_home" ]; then
                AVAILABLE_17=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "")
                if [ -n "$AVAILABLE_17" ]; then
                    echo "  Java 17 disponible en: $AVAILABLE_17"
                fi
            fi
        fi
    else
        print_error "Java no encontrado. Por favor instala Java 17"
        exit 1
    fi
    
    # Maven
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
        print_success "Maven $MVN_VERSION encontrado"
    else
        print_error "Maven no encontrado"
        exit 1
    fi
    
    # PostgreSQL
    if command -v psql &> /dev/null; then
        print_success "PostgreSQL encontrado"
    else
        print_warning "PostgreSQL client no encontrado. Asegúrate de tener PostgreSQL instalado"
    fi
    
    echo ""
}

# Función para limpiar el proyecto
clean() {
    print_header "Limpiando proyecto..."
    mvn clean
    print_success "Proyecto limpiado"
}

# Función para compilar
build() {
    print_header "Compilando proyecto..."
    mvn clean install -DskipTests
    print_success "Compilación exitosa"
}

# Función para compilar con tests
build_with_tests() {
    print_header "Compilando proyecto con tests..."
    mvn clean install
    print_success "Compilación y tests exitosos"
}

# Función para ejecutar tests
test() {
    print_header "Ejecutando tests..."
    mvn test
    print_success "Tests completados"
}

# Función para verificar formato
check_format() {
    print_header "Verificando formato de código..."
    mvn fmt:check
    print_success "Formato correcto"
}

# Función para formatear código
format() {
    print_header "Formateando código..."
    mvn fmt:format
    print_success "Código formateado"
}

# Función para ejecutar la aplicación
run() {
    print_header "Ejecutando aplicación (perfil localhost)..."
    mvn spring-boot:run -Dspring-boot.run.profiles=localhost
}

# Función para ejecutar con perfil específico
run_profile() {
    PROFILE=$1
    print_header "Ejecutando aplicación (perfil $PROFILE)..."
    mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE
}

# Función para crear base de datos
create_db() {
    print_header "Creando base de datos..."
    echo "Host: localhost"
    echo "Puerto: 5433"
    echo "Base de datos: ebill-dev-cus"
    echo "Usuario: postgres"
    echo ""
    
    createdb -h localhost -p 5433 -U postgres ebill-dev-cus && \
        print_success "Base de datos creada exitosamente" || \
        print_error "Error al crear base de datos. Verifica que PostgreSQL esté corriendo"
}

# Función para eliminar base de datos
drop_db() {
    print_warning "Esta acción eliminará TODOS los datos de la base de datos"
    read -p "¿Estás seguro? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        dropdb -h localhost -p 5433 -U postgres ebill-dev-cus && \
            print_success "Base de datos eliminada" || \
            print_error "Error al eliminar base de datos"
    else
        print_warning "Operación cancelada"
    fi
}

# Función para reiniciar base de datos
reset_db() {
    print_header "Reiniciando base de datos..."
    drop_db
    create_db
    print_success "Base de datos reiniciada. Ejecuta la aplicación para aplicar migraciones"
}

# Función para verificar estado del puerto
check_port() {
    PORT=${1:-8180}
    print_header "Verificando puerto $PORT..."
    
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
        PID=$(lsof -ti:$PORT)
        print_warning "Puerto $PORT está en uso por proceso PID: $PID"
        echo ""
        echo "Detalles del proceso:"
        ps -p $PID -o pid,user,command
        echo ""
        read -p "¿Deseas matar el proceso? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            kill -9 $PID && print_success "Proceso terminado"
        fi
    else
        print_success "Puerto $PORT está libre"
    fi
}

# Función para verificar PostgreSQL
check_postgres() {
    print_header "Verificando PostgreSQL..."
    
    if pg_isready -h localhost -p 5433 >/dev/null 2>&1; then
        print_success "PostgreSQL está corriendo"
        
        # Verificar si la base de datos existe
        if psql -h localhost -p 5433 -U postgres -lqt | cut -d \| -f 1 | grep -qw ebill-dev-cus; then
            print_success "Base de datos ebill-dev-cus existe"
        else
            print_warning "Base de datos ebill-dev-cus no existe"
            echo "Ejecuta: ./ebill.sh create-db"
        fi
    else
        print_error "PostgreSQL no está corriendo en localhost:5433"
        echo ""
        echo "Para iniciar PostgreSQL:"
        echo "  macOS: brew services start postgresql@15"
        echo "  Linux: sudo systemctl start postgresql"
        echo "  Docker: docker start postgres-ebill"
    fi
}

# Función para mostrar logs
logs() {
    print_header "Mostrando logs de la aplicación..."
    if [ -f "logs/application.log" ]; then
        tail -f logs/application.log
    else
        print_warning "No se encontró archivo de logs"
        echo "Los logs aparecerán en la consola al ejecutar la aplicación"
    fi
}

# Función para generar JAR
package() {
    print_header "Generando JAR..."
    mvn clean package -DskipTests
    
    if [ -f "target/ebill-customer-1.0.0.jar" ]; then
        print_success "JAR generado exitosamente"
        echo ""
        echo "Ubicación: target/ebill-customer-1.0.0.jar"
        echo ""
        echo "Para ejecutar:"
        echo "  java -jar -Dspring.profiles.active=localhost target/ebill-customer-1.0.0.jar"
    fi
}

# Función para verificar configuración completa
health_check() {
    print_header "Verificación Completa del Sistema"
    echo ""
    
    check_prerequisites
    check_postgres
    check_port 8180
    
    echo ""
    print_header "Estado del Proyecto"
    
    # Verificar archivo .env
    if [ -f ".env" ]; then
        print_success "Archivo .env existe"
    else
        print_warning "Archivo .env no existe. Copia .env.example a .env"
    fi
    
    # Verificar dependencias de Maven
    if [ -d "target" ]; then
        print_success "Directorio target existe (proyecto compilado)"
    else
        print_warning "Proyecto no compilado. Ejecuta: ./ebill.sh build"
    fi
    
    echo ""
}

# Función para actualizar dependencias
update_deps() {
    print_header "Actualizando dependencias de Maven..."
    mvn clean install -U
    print_success "Dependencias actualizadas"
}

# Función de ayuda
show_help() {
    cat << EOF
${BLUE}eBill Customer - Script de Utilidades${NC}

${YELLOW}Uso:${NC}
  ./ebill.sh [comando]

${YELLOW}Comandos disponibles:${NC}

  ${GREEN}Compilación y Ejecución:${NC}
    build                 Compilar proyecto (sin tests)
    build-test            Compilar proyecto con tests
    run                   Ejecutar aplicación (perfil localhost)
    run-profile [perfil]  Ejecutar con perfil específico
    package               Generar JAR ejecutable
    test                  Ejecutar tests
    clean                 Limpiar proyecto

  ${GREEN}Formato de Código:${NC}
    format                Formatear código automáticamente
    check-format          Verificar formato de código

  ${GREEN}Base de Datos:${NC}
    create-db             Crear base de datos
    drop-db               Eliminar base de datos
    reset-db              Reiniciar base de datos (drop + create)
    check-postgres        Verificar estado de PostgreSQL

  ${GREEN}Utilidades:${NC}
    check                 Verificar prerequisitos
    health                Verificación completa del sistema
    check-port [puerto]   Verificar estado del puerto (default: 8180)
    logs                  Mostrar logs de la aplicación
    update-deps           Actualizar dependencias de Maven
    help                  Mostrar esta ayuda

${YELLOW}Ejemplos:${NC}
  ./ebill.sh build                  # Compilar proyecto
  ./ebill.sh run                    # Ejecutar aplicación
  ./ebill.sh reset-db               # Reiniciar base de datos
  ./ebill.sh health                 # Verificar todo el sistema
  ./ebill.sh run-profile production # Ejecutar con perfil production

${YELLOW}Documentación:${NC}
  - Inicio Rápido: QUICKSTART.md
  - Configuración Completa: SETUP_AND_ANALYSIS.md
  - Ejemplos de API: API_EXAMPLES.md
  - README Actualizado: README_NEW.md

EOF
}

# Main script
case "$1" in
    check)
        check_prerequisites
        ;;
    clean)
        clean
        ;;
    build)
        build
        ;;
    build-test)
        build_with_tests
        ;;
    test)
        test
        ;;
    format)
        format
        ;;
    check-format)
        check_format
        ;;
    run)
        run
        ;;
    run-profile)
        if [ -z "$2" ]; then
            print_error "Debes especificar un perfil"
            echo "Ejemplo: ./ebill.sh run-profile localhost"
            exit 1
        fi
        run_profile $2
        ;;
    create-db)
        create_db
        ;;
    drop-db)
        drop_db
        ;;
    reset-db)
        reset_db
        ;;
    check-port)
        check_port ${2:-8180}
        ;;
    check-postgres)
        check_postgres
        ;;
    logs)
        logs
        ;;
    package)
        package
        ;;
    health)
        health_check
        ;;
    update-deps)
        update_deps
        ;;
    help|--help|-h|"")
        show_help
        ;;
    *)
        print_error "Comando desconocido: $1"
        echo ""
        show_help
        exit 1
        ;;
esac



