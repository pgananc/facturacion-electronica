# 🛠️ Guía de Desarrollo - eBill Product

Esta guía proporciona información detallada para desarrolladores que trabajan en el microservicio ebill-product.

## 🎯 Inicio Rápido para Desarrolladores

### Setup Inicial (Primera vez)

```bash
# 1. Clonar el repositorio
git clone http://85.239.237.88:81/dev-billing/ebill-product.git
cd ebill-product

# 2. Instalar extensiones de Cursor IDE (recomendadas)
# El IDE te preguntará automáticamente cuando abras el proyecto

# 3. Levantar base de datos
docker-compose up -d

# 4. Compilar el proyecto
mvn clean install

# 5. Ejecutar
./run.sh localhost
```

## 📐 Arquitectura del Proyecto

### Patrón de Capas

```
┌─────────────────────────────────────────┐
│         Controller Layer                │
│  (REST API Endpoints)                   │
│  - ProductController                    │
└─────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│          Service Layer                  │
│  (Business Logic)                       │
│  - ProductServiceImpl                   │
└─────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│       Repository Layer                  │
│  (Data Access)                          │
│  - IProductRepo                         │
└─────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────┐
│         Database Layer                  │
│  (PostgreSQL)                           │
└─────────────────────────────────────────┘
```

### Componentes Principales

- **DTOs**: Objetos de transferencia de datos (ProductDto, TaxTypeDto)
- **Entities**: Entidades JPA (Product, TaxType)
- **Mappers**: MapStruct para conversión entre DTOs y Entities
- **Responses**: Estructuras de respuesta personalizadas
- **Exceptions**: Manejo centralizado de excepciones

## 🔍 Convenciones de Código

### Estilo de Código

- **Indentación**: 2 espacios (configurado en java-formatter.xml)
- **Longitud de línea**: 100 caracteres máximo
- **Formateo**: Google Java Format (automático al guardar)

### Nomenclatura

```java
// Clases: PascalCase
public class ProductServiceImpl { }

// Métodos: camelCase
public ProductDto getProductById(Long id) { }

// Constantes: UPPER_SNAKE_CASE
private static final String DEFAULT_STATUS = "ACTIVE";

// Variables: camelCase
private Long idProduct;
```

### Anotaciones de Lombok

El proyecto usa Lombok extensivamente:

```java
@Data                    // Getters, Setters, toString, equals, hashCode
@Builder                 // Patrón Builder
@NoArgsConstructor      // Constructor sin argumentos
@AllArgsConstructor     // Constructor con todos los argumentos
@RequiredArgsConstructor // Constructor con campos final
@Slf4j                  // Logger automático
@FieldDefaults(level = PRIVATE) // Todos los campos private por defecto
```

### Logging

Usa el logger de Lombok (@Slf4j) y las utilidades de ebill-utils:

```java
@Slf4j
public class ProductController {
    
    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        
        // Log de entrada
        setLogs(
            EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName()),
            EventType.REQUEST,
            Level.LEVEL_001
        );
        
        // Lógica del negocio
        ProductDto product = productService.getProductById(id);
        
        // Log de salida
        setLogs(
            EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(product),
            EventType.RESPONSE,
            Level.LEVEL_001
        );
        
        return response;
    }
}
```

## 🧪 Testing

### Estructura de Tests

```bash
src/test/java/com/wallparisoft/ebill/product/
├── controller/          # Tests de controladores
├── service/             # Tests de servicios
└── repository/          # Tests de repositorios
```

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Tests específicos
mvn test -Dtest=ProductServiceImplTest

# Con cobertura
mvn clean test jacoco:report
```

### Ejemplo de Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private IProductService productService;
    
    @Test
    void testGetProductById() throws Exception {
        // Arrange
        Long productId = 1L;
        ProductDto expectedProduct = ProductDto.builder()
            .idProduct(productId)
            .name("Test Product")
            .build();
            
        when(productService.getProductById(productId))
            .thenReturn(expectedProduct);
        
        // Act & Assert
        mockMvc.perform(get("/api/product/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productDto.idProduct").value(productId))
            .andExpect(jsonPath("$.productDto.name").value("Test Product"));
    }
}
```

## 🗄️ Trabajar con Base de Datos

### Conexión a la Base de Datos

```bash
# Docker
docker exec -it ebill-postgres-dev psql -U postgres -d ebill-dev-prod

# O con psql local
psql -h localhost -p 5433 -U postgres -d ebill-dev-prod
```

### Comandos útiles de PostgreSQL

```sql
-- Ver todas las tablas
\dt

-- Describir tabla
\d product

-- Ver datos de product
SELECT * FROM product LIMIT 10;

-- Ver tipos de impuesto
SELECT * FROM tax_type;

-- Ver historial de migraciones Flyway
SELECT * FROM flyway ORDER BY installed_rank;
```

### Crear Nueva Migración Flyway

1. Crea un archivo en `src/main/resources/flyway/`
2. Nomenclatura: `V{YYYYMMDDNN}__{descripcion}.sql`
   - Ejemplo: `V2025101200__add_category_to_product.sql`

```sql
-- V2025101200__add_category_to_product.sql
ALTER TABLE product ADD COLUMN category VARCHAR(100);

CREATE INDEX idx_product_category ON product(category);

COMMENT ON COLUMN product.category IS 'Categoría del producto';
```

3. La migración se ejecutará automáticamente al iniciar la aplicación

## 🔐 Seguridad y Autenticación

El proyecto usa Spring Security con dependencias custom:

- **ebill-auth-util**: Proporciona configuración de seguridad
- **SecurityConfig**: Configuración centralizada de seguridad

### Configuración de Seguridad

```java
@Import({SecurityConfig.class, AuthUtilConfig.class})
public class EBillProductApplication { }
```

### Endpoints Protegidos

Todos los endpoints bajo `/api/**` requieren autenticación JWT.

## 🚀 Deployment

### Construir JAR

```bash
# Producción
mvn clean package -Pprod

# Con skip de tests
mvn clean package -DskipTests
```

### Variables de Entorno para Producción

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=production-db-host
export DB_PORT=5432
export DB_NAME=ebill_prod
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
```

## 🐛 Debugging

### Debug Remoto

Si la aplicación está corriendo en un servidor remoto:

1. Inicia la app con opciones de debug:
   ```bash
   java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
        -Dspring.profiles.active=prod \
        ebill-product-1.0.0.jar
   ```

2. En Cursor IDE, crea una configuración de debug remoto:
   ```json
   {
     "type": "java",
     "name": "Debug Remote",
     "request": "attach",
     "hostName": "remote-server-ip",
     "port": 5005
   }
   ```

### Logs en Tiempo Real

```bash
# Con Docker
docker-compose logs -f

# Con tail (si hay archivo de log)
tail -f logs/application.log

# Filtrar logs por nivel
tail -f logs/application.log | grep ERROR
```

## 📦 Gestión de Dependencias

### Actualizar Dependencias

```bash
# Ver dependencias desactualizadas
mvn versions:display-dependency-updates

# Actualizar versiones
mvn versions:use-latest-releases
```

### Resolver Conflictos de Dependencias

```bash
# Ver árbol de dependencias
mvn dependency:tree

# Analizar conflictos
mvn dependency:tree -Dverbose

# Ver dependencias específicas
mvn dependency:tree -Dincludes=org.springframework.boot
```

## 🔄 Git Workflow

### Ramas

- `main` - Producción
- `develop` - Desarrollo
- `feature/*` - Nuevas características
- `bugfix/*` - Corrección de bugs
- `hotfix/*` - Correcciones urgentes en producción

### Commits

Usa mensajes descriptivos:

```bash
# Formato recomendado
git commit -m "feat: agregar endpoint para buscar productos por categoría"
git commit -m "fix: corregir validación de precio unitario"
git commit -m "docs: actualizar README con instrucciones de deployment"
git commit -m "refactor: optimizar consulta de productos paginados"
```

Prefijos:
- `feat:` - Nueva característica
- `fix:` - Corrección de bug
- `docs:` - Documentación
- `refactor:` - Refactorización
- `test:` - Tests
- `chore:` - Tareas de mantenimiento

## 🛠️ Herramientas Útiles

### IntelliJ IDEA / Cursor IDE Plugins

- **Lombok** - Soporte para anotaciones Lombok
- **MapStruct** - Soporte para MapStruct
- **Spring Boot Tools** - Herramientas para Spring Boot
- **Database Navigator** - Cliente de base de datos
- **GitToolBox** - Herramientas adicionales de Git

### Comandos Maven Útiles

```bash
# Limpiar proyecto
mvn clean

# Compilar sin tests
mvn compile -DskipTests

# Ver información del proyecto
mvn help:effective-pom
mvn help:effective-settings

# Generar sitio con documentación
mvn site

# Verificar estilo de código
mvn fmt:check

# Formatear código
mvn fmt:format
```

## 📊 Monitoreo y Métricas

### Spring Boot Actuator (si está habilitado)

```bash
# Health check
curl http://localhost:8380/actuator/health

# Métricas
curl http://localhost:8380/actuator/metrics

# Info de la aplicación
curl http://localhost:8380/actuator/info
```

## 🤝 Code Review Checklist

Antes de crear un Merge Request:

- [ ] El código compila sin errores
- [ ] Todos los tests pasan
- [ ] El código está formateado correctamente
- [ ] Se agregaron tests para nuevo código
- [ ] Se actualizó la documentación si es necesario
- [ ] No hay credenciales hardcodeadas
- [ ] Se revisaron los logs de debug
- [ ] Se probó manualmente la funcionalidad
- [ ] No hay dependencias innecesarias
- [ ] El código sigue las convenciones del proyecto

## 📚 Recursos Adicionales

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [MapStruct Documentation](https://mapstruct.org/)
- [Lombok Features](https://projectlombok.org/features/)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

**Happy Coding! 💻✨**

