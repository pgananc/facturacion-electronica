# 🔒 Validación de MATRIZ Única - Backend (OPCIONAL)

## 📋 Resumen

El **frontend ya maneja completamente** la lógica de MATRIZ única, pero se recomienda que el backend agregue una validación adicional como **capa extra de seguridad**.

---

## ✅ Lo que YA hace el Frontend

1. **Detección automática**: Cuando el usuario selecciona "MATRIZ", verifica si ya existe otra MATRIZ
2. **Warning Dialog**: Muestra información de la MATRIZ actual y pide confirmación
3. **Cambio automático en 2 pasos**:
   - **Paso 1**: `PUT /api/establishments/{id}` → Cambiar MATRIZ actual a "SUCURSAL"
   - **Paso 2**: `POST` o `PUT` → Crear/actualizar la nueva MATRIZ

---

## 🛡️ Validación Recomendada en Backend (OPCIONAL)

### Objetivo
Prevenir que existan 2 MATRIZ activas simultáneamente, incluso si alguien usa la API directamente (sin pasar por el frontend).

### Implementación Sugerida

#### 1. En el Service o Controller de `Establishment`

```java
@Service
public class EstablishmentService {
    
    @Autowired
    private EstablishmentRepository establishmentRepository;
    
    /**
     * Valida que no exista otra MATRIZ activa antes de crear/actualizar
     */
    private void validateUniqueMatriz(EstablishmentRequestDto request, Long currentEstablishmentId) {
        // Solo validar si el tipo es MATRIZ
        if (!"MATRIZ".equals(request.getType())) {
            return;
        }
        
        // Buscar si ya existe una MATRIZ para esta compañía
        Optional<Establishment> existingMatriz = establishmentRepository
            .findByCompanyIdAndType(request.getCompanyId(), "MATRIZ");
        
        // Si existe y NO es el mismo registro que estamos actualizando
        if (existingMatriz.isPresent() && 
            !existingMatriz.get().getIdEstablishment().equals(currentEstablishmentId)) {
            throw new BusinessException(
                "Ya existe una MATRIZ para esta compañía. " +
                "Solo puede haber una MATRIZ activa por compañía."
            );
        }
    }
    
    /**
     * Crear una nueva sucursal
     */
    public EstablishmentResponseDto create(EstablishmentRequestDto request) {
        // Validar MATRIZ única
        validateUniqueMatriz(request, null);
        
        // ... resto de la lógica de creación
    }
    
    /**
     * Actualizar una sucursal existente
     */
    public EstablishmentResponseDto update(Long id, EstablishmentRequestDto request) {
        // Validar MATRIZ única (pasamos el ID actual para excluirlo)
        validateUniqueMatriz(request, id);
        
        // ... resto de la lógica de actualización
    }
}
```

#### 2. En el Repository

```java
@Repository
public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {
    
    /**
     * Buscar MATRIZ de una compañía
     */
    Optional<Establishment> findByCompanyIdAndType(Long companyId, String type);
    
    /**
     * Alternativa: Buscar todas las sucursales de una compañía por tipo
     */
    List<Establishment> findByIdCompanyAndType(Long idCompany, String type);
}
```

#### 3. Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse error = new ErrorResponse(
            "error",
            ex.getMessage(),
            Collections.emptyList()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
```

---

## 🔄 Flujo Completo (Frontend + Backend)

### Escenario: Usuario quiere cambiar Sucursal A a MATRIZ (ya existe MATRIZ B)

#### Frontend:
1. Usuario selecciona "MATRIZ" → Detecta que existe MATRIZ B
2. Muestra warning con datos de MATRIZ B
3. Usuario confirma
4. **Request 1**: `PUT /api/establishments/{idMatrizB}` con `type: "SUCURSAL"`
   - Backend: ✅ Permite (está cambiando MATRIZ → SUCURSAL)
5. **Request 2**: `PUT /api/establishments/{idSucursalA}` con `type: "MATRIZ"`
   - Backend: ✅ Permite (ya no existe otra MATRIZ activa)

#### Si alguien usa la API directamente (sin el frontend):
1. **Request directo**: `PUT /api/establishments/{idSucursalA}` con `type: "MATRIZ"`
2. Backend detecta que ya existe MATRIZ B
3. Backend rechaza con error 400: "Ya existe una MATRIZ para esta compañía"

---

## 📊 Comparación

| Aspecto | Solo Frontend | Frontend + Backend |
|---------|---------------|-------------------|
| **UX amigable** | ✅ Warning, confirmación | ✅ Warning, confirmación |
| **Previene errores del usuario** | ✅ Sí | ✅ Sí |
| **Protege contra API directa** | ❌ No | ✅ Sí |
| **Previene condiciones de carrera** | ⚠️ Bajo riesgo | ✅ Protegido |
| **Complejidad** | Simple | Media |

---

## 🎯 Recomendación Final

### ✅ Implementar validación en Backend SI:
- Múltiples usuarios pueden gestionar sucursales simultáneamente
- Existe API pública o múltiples clientes frontend
- Requieren máxima integridad de datos
- Sistema crítico de facturación

### ⚠️ NO es urgente SI:
- Solo un administrador gestiona sucursales a la vez
- Frontend es el único cliente de la API
- Backend confía 100% en el frontend
- Prioridad es velocidad de desarrollo

---

## 🧪 Casos de Prueba Backend (si implementan validación)

```java
@Test
public void testCannotCreateTwoMatrices() {
    // Arrange: Ya existe una MATRIZ
    Establishment matriz1 = createMatriz(companyId, "001", "Matriz Principal");
    establishmentRepository.save(matriz1);
    
    // Act: Intentar crear otra MATRIZ
    EstablishmentRequestDto request = new EstablishmentRequestDto();
    request.setType("MATRIZ");
    request.setCompanyId(companyId);
    
    // Assert: Debe lanzar excepción
    assertThrows(BusinessException.class, () -> {
        establishmentService.create(request);
    });
}

@Test
public void testCanUpdateMatrizToSucursal() {
    // Arrange: Ya existe una MATRIZ
    Establishment matriz = createMatriz(companyId, "001", "Matriz Principal");
    establishmentRepository.save(matriz);
    
    // Act: Cambiar MATRIZ a SUCURSAL
    EstablishmentRequestDto request = new EstablishmentRequestDto();
    request.setType("SUCURSAL");
    request.setCompanyId(companyId);
    
    // Assert: Debe permitir el cambio
    assertDoesNotThrow(() -> {
        establishmentService.update(matriz.getIdEstablishment(), request);
    });
}

@Test
public void testCanUpdateSucursalToMatrizWhenNoMatrizExists() {
    // Arrange: No existe MATRIZ, solo sucursales
    Establishment sucursal = createSucursal(companyId, "002", "Sucursal Guayaquil");
    establishmentRepository.save(sucursal);
    
    // Act: Cambiar SUCURSAL a MATRIZ
    EstablishmentRequestDto request = new EstablishmentRequestDto();
    request.setType("MATRIZ");
    request.setCompanyId(companyId);
    
    // Assert: Debe permitir el cambio (no hay otra MATRIZ)
    assertDoesNotThrow(() -> {
        establishmentService.update(sucursal.getIdEstablishment(), request);
    });
}
```

---

## 📞 Resumen para el Backend

**¿Necesitan implementar algo?**

- **Mínimo (YA FUNCIONA)**: Nada. El frontend maneja todo.
- **Recomendado (SEGURIDAD EXTRA)**: Agregar validación en `create()` y `update()` que rechace si ya existe otra MATRIZ activa.

**Tiempo estimado si implementan**: 30-45 minutos (código + tests)

**Beneficio**: Protección contra uso directo de API o condiciones de carrera.

---

**✅ FRONTEND: 100% COMPLETO Y FUNCIONAL**  
**⚠️ BACKEND: OPCIONAL (pero recomendado para seguridad)**
