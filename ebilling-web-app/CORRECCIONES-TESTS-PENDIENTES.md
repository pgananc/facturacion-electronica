# 🔧 Correcciones Pendientes en Tests

## Resumen de Errores Encontrados

### ✅ Corregidos

- `permission.service.spec.ts` - Mayoría de errores corregidos
- `permission.guard.spec.ts` - Mayoría de errores corregidos
- Helper de mocks creado (`_test-helpers/mock-data.ts`)

### ⚠️ Pendientes

#### 1. `permission.guard.spec.ts`

- [ ] Reemplazar todas las instancias de `activeSubscription` con `createMockCompanySubscription()`
- [ ] Corregir test de cache de errores (línea 389)

#### 2. `client.service.spec.ts`

- [ ] `ClientSaveDto` no tiene `email`, usar `contacts` array
- [ ] `ClientResponse` tiene `clientDtos` array, no propiedades directas
- [ ] `ClientRequestDto` requiere todas las propiedades: `name`, `clientType`, `status`, `companyIdentification`

#### 3. `invoice.service.spec.ts`

- [ ] `InvoiceDto` requiere más propiedades: `idCompany`, `issueDate`, `discount`, `tip`, `establishment`
- [ ] `InvoiceSearchDto` no tiene `identification`, usar `buyerIdentification`
- [ ] `DocumentQueryDto.documentType` es `number` (0, 1, 2) no `string`
- [ ] `InvoiceSearchForDebitNoteDto` y `InvoiceSearchForCreditNoteDto` requieren `idCompany` y tienen propiedades diferentes

#### 4. `has-permission.directive.spec.ts`

- [ ] Parece estar bien, pero verificar que compile

## Comandos para Corregir

### Opción 1: Corregir manualmente usando los helpers

```typescript
// En lugar de:
const mockSubscription: CompanySubscription = {
  id: 1,
  companyId: 1,
  // ...
};

// Usar:
import { createMockCompanySubscription } from "../_test-helpers/mock-data";
const mockSubscription = createMockCompanySubscription();
```

### Opción 2: Ejecutar tests y corregir errores uno por uno

```bash
npm test
# Ver errores y corregir según se muestren
```

## Próximos Pasos

1. Corregir `client.service.spec.ts` con las propiedades correctas
2. Corregir `invoice.service.spec.ts` con los DTOs correctos
3. Verificar que `has-permission.directive.spec.ts` compile
4. Ejecutar tests nuevamente para verificar que todos pasen
