# Plan de Acción - Implementación de Rate Limiting

## 🎯 Objetivo
Implementar rate limiting configurable desde base de datos para 4 endpoints críticos según especificaciones.

## 📅 Fases de Implementación

### Fase 1: Configuración Base (1-2 días)
**Prioridad: ALTA**

#### Tareas:
1. ✅ Crear migración Flyway con parámetros
   - Archivo: `V2025010200__add_rate_limiting_params.sql`
   - Verificar IDs no conflictivos
   - Ejecutar en desarrollo y verificar

2. ✅ Agregar enums a ParmsAuthEnum
   - Códigos COD010-COD025
   - Compilar y verificar

3. ✅ Crear utilidad para extraer IP
   - Archivo: `RequestUtils.java`
   - Refactorizar controladores existentes

**Criterio de Éxito:** Parámetros en BD, enums compilando, utilidad funcionando

---

### Fase 2: Servicio Genérico (2-3 días)
**Prioridad: ALTA**

#### Tareas:
1. ✅ Crear `GenericRateLimitService`
   - Métodos: `canMakeRequest`, `incrementRequest`, `getTimeUntilNextAttempt`
   - Soporte para dimensiones: EMAIL, IP, CLIENT
   - Lectura de parámetros desde BD con cache

2. ✅ Tests unitarios del servicio
   - Validar lógica de rate limiting
   - Validar TTL en Redis
   - Validar múltiples dimensiones

**Criterio de Éxito:** Servicio genérico funcionando, tests pasando

---

### Fase 3: Implementación por Endpoint (3-4 días)
**Prioridad: ALTA**

#### 3.1 `/api/mail/send` (1 día)
- Modificar `MailController` para recibir `HttpServletRequest`
- Modificar `UserServiceImpl.sendMailUser` para validar rate limiting
- Implementar validación por EMAIL e IP
- Tests de integración

#### 3.2 `/api/token/client` (1 día)
- Extender `ClientTokenServiceImpl.saveClientToken`
- Agregar validación por IP (ya tiene por email)
- Validar ambos límites
- Tests de integración

#### 3.3 `/api/token/validate-client/generate` (1 día)
- **NUEVO:** Implementar rate limiting completo
- Validar por IP (10/15min) y Cliente (20/1h)
- Incrementar solo en fallos
- Tests de integración

#### 3.4 `/api/token/resend-client` (1 día)
- Extender `ClientTokenServiceImpl.resendClientToken`
- Agregar validación por IP (ya tiene por email)
- Validar ambos límites
- Tests de integración

**Criterio de Éxito:** Todos los endpoints con rate limiting, tests pasando

---

### Fase 4: Testing y Ajustes (2-3 días)
**Prioridad: MEDIA**

#### Tareas:
1. ✅ Tests de integración completos
2. ✅ Tests de carga
3. ✅ Ajustar valores según resultados
4. ✅ Documentar comportamiento

**Criterio de Éxito:** Todos los tests pasando, valores ajustados

---

### Fase 5: Monitoreo y Deployment (1-2 días)
**Prioridad: MEDIA**

#### Tareas:
1. ✅ Configurar métricas
2. ✅ Configurar alertas
3. ✅ Deploy a staging
4. ✅ Monitoreo activo
5. ✅ Deploy a producción (gradual)

**Criterio de Éxito:** En producción, monitoreado, funcionando

---

## 🔍 Puntos Críticos de Revisión

### Antes de cada fase:
- [ ] Revisar código con equipo
- [ ] Validar tests pasan
- [ ] Verificar que no rompe funcionalidad existente

### Durante implementación:
- [ ] Validar que rate limiting se aplica ANTES de procesar
- [ ] Verificar manejo de errores (valores por defecto)
- [ ] Confirmar que Redis TTL funciona correctamente
- [ ] Verificar que múltiples dimensiones funcionan juntas

### Después de implementación:
- [ ] Tests manuales de cada endpoint
- [ ] Verificar logs y métricas
- [ ] Validar que mensajes de error son claros
- [ ] Confirmar que no hay degradación de performance

---

## 🚨 Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Valores muy restrictivos bloquean usuarios legítimos | ALTO | Empezar con valores de la propuesta, ajustar según uso real |
| Redis se queda sin memoria | MEDIO | Monitorear uso, configurar eviction policy |
| Performance degradado | MEDIO | Cachear parámetros, optimizar consultas Redis |
| IP no se detecta correctamente | MEDIO | Probar con proxies/load balancers, logging detallado |
| Rate limiting no funciona en producción | ALTO | Tests exhaustivos, feature flag para deshabilitar |

---

## 📊 Métricas de Éxito

### Técnicas:
- ✅ Todos los tests pasan
- ✅ No hay errores en logs relacionados con rate limiting
- ✅ Latencia aumentada < 50ms por validación
- ✅ Redis memory usage < 10% del total

### Funcionales:
- ✅ Rate limiting funciona para todos los endpoints
- ✅ Límites se respetan correctamente
- ✅ Mensajes de error son claros
- ✅ Usuarios legítimos no son bloqueados incorrectamente

---

## 🛠️ Herramientas Necesarias

- **Desarrollo:**
  - IDE con soporte Java/Spring
  - Redis local o Docker
  - Postman/Insomnia para testing

- **Testing:**
  - JUnit para tests unitarios
  - TestContainers para Redis en tests
  - JMeter para carga

- **Monitoreo:**
  - Redis CLI para debugging
  - Logs de aplicación
  - Métricas de aplicación (si disponible)

---

## 📝 Checklist Final

Antes de considerar la implementación completa:

### Código:
- [ ] Todos los endpoints tienen rate limiting
- [ ] Código está documentado
- [ ] No hay código duplicado
- [ ] Manejo de errores robusto

### Testing:
- [ ] Tests unitarios > 80% cobertura
- [ ] Tests de integración pasan
- [ ] Tests manuales completados
- [ ] Tests de carga ejecutados

### Configuración:
- [ ] Parámetros en base de datos
- [ ] Valores ajustados según uso
- [ ] Documentación actualizada
- [ ] Enums actualizados

### Deployment:
- [ ] Deployado en staging
- [ ] Probado en staging
- [ ] Monitoreo configurado
- [ ] Plan de rollback preparado

---

## 🎓 Lecciones Aprendidas (Actualizar después de implementar)

- [ ] ¿Qué funcionó bien?
- [ ] ¿Qué se podría mejorar?
- [ ] ¿Valores de rate limits fueron apropiados?
- [ ] ¿Hubo problemas inesperados?

---

## 📞 Contactos y Recursos

- **Documentación:** Ver `PROPUESTA_RATE_LIMITING.md` y `RECOMENDACIONES_IMPLEMENTACION_RATE_LIMITING.md`
- **Código de referencia:** `ClientTokenRateLimitService.java`
- **Ejemplo de extracción de IP:** `LoginController.extractClientIp()`

---

**Última actualización:** [Fecha]
**Estado:** Propuesta - Pendiente de aprobación

