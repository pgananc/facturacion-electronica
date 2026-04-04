# Makefile para ebill-product
# Facilita la ejecución de comandos comunes

.PHONY: help install clean compile test run run-dev run-prod docker-up docker-down docker-logs format check-format package build

# Variables
MAVEN := mvn
DOCKER_COMPOSE := docker-compose
PROFILE := localhost

## help: Muestra esta ayuda
help:
	@echo "Comandos disponibles para ebill-product:"
	@echo ""
	@sed -n 's/^##//p' ${MAKEFILE_LIST} | column -t -s ':' | sed -e 's/^/ /'

## install: Instala las dependencias y compila el proyecto
install:
	$(MAVEN) clean install

## clean: Limpia el proyecto
clean:
	$(MAVEN) clean

## compile: Compila el código fuente
compile:
	$(MAVEN) compile

## test: Ejecuta los tests
test:
	$(MAVEN) test

## test-coverage: Ejecuta tests con cobertura
test-coverage:
	$(MAVEN) clean test jacoco:report

## run: Ejecuta la aplicación con perfil localhost
run:
	@echo "Ejecutando con perfil: $(PROFILE)"
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=$(PROFILE)

## run-dev: Ejecuta con perfil dev
run-dev:
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=dev

## run-prod: Ejecuta con perfil prod
run-prod:
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=prod

## package: Empaqueta la aplicación en JAR
package:
	$(MAVEN) clean package -DskipTests

## build: Construye la aplicación completa con tests
build:
	$(MAVEN) clean package

## docker-up: Inicia PostgreSQL con Docker Compose
docker-up:
	$(DOCKER_COMPOSE) up -d
	@echo "Esperando a que PostgreSQL esté listo..."
	@sleep 5
	@echo "PostgreSQL está listo en localhost:5433"

## docker-down: Detiene y elimina los contenedores
docker-down:
	$(DOCKER_COMPOSE) down

## docker-logs: Muestra los logs de PostgreSQL
docker-logs:
	$(DOCKER_COMPOSE) logs -f postgres

## docker-restart: Reinicia PostgreSQL
docker-restart: docker-down docker-up

## format: Formatea el código
format:
	$(MAVEN) fmt:format

## check-format: Verifica el formato del código
check-format:
	$(MAVEN) fmt:check

## dependency-tree: Muestra el árbol de dependencias
dependency-tree:
	$(MAVEN) dependency:tree

## update-dependencies: Muestra dependencias desactualizadas
update-dependencies:
	$(MAVEN) versions:display-dependency-updates

## flyway-info: Muestra el estado de las migraciones
flyway-info:
	$(MAVEN) flyway:info

## flyway-migrate: Ejecuta las migraciones pendientes
flyway-migrate:
	$(MAVEN) flyway:migrate

## flyway-clean: Limpia la base de datos (¡CUIDADO!)
flyway-clean:
	@echo "⚠️  ADVERTENCIA: Esto eliminará todos los datos de la base de datos!"
	@read -p "¿Estás seguro? [y/N]: " confirm && [ $$confirm = y ] || exit 1
	$(MAVEN) flyway:clean

## dev-setup: Configuración completa para desarrollo (primera vez)
dev-setup: docker-up install
	@echo "✅ Setup completado!"
	@echo "Ejecuta 'make run' para iniciar la aplicación"

## quick-start: Inicio rápido (Docker + Run)
quick-start: docker-up run

## lint: Verifica el código
lint: check-format
	@echo "Verificando código..."

## all: Limpia, compila, ejecuta tests y empaqueta
all: clean compile test package
	@echo "✅ Build completo exitoso!"

