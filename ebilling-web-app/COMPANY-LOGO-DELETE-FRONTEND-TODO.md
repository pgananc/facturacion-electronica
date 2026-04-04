# TODO: Eliminar logo de compañía (frontend)

Backend ya expone: `DELETE /api/company/{idCompany}/logo`  
Respuesta 200: `{ "message": "Logo eliminado exitosamente" }` (idempotente si no hay logo).

---

## 1. CompanyService

- [ ] Añadir método `deleteLogo(idCompany: number): Observable<{ message: string }>` (o `Observable<void>` si solo importa el 200).
- [ ] Llamar a `this.http.delete<...>(`${this.url}/${idCompany}/logo`)`.
- [ ] (Opcional) Añadir test en `company.service.spec.ts` para `deleteLogo`: verificar método DELETE y URL.

---

## 2. UpdateCompanyComponent – estado

- [ ] Añadir propiedad `userRemovedLogo = false`.
- [ ] Poner `userRemovedLogo = false` al iniciar (ngOnInit) y al cargar compañía en edición.
- [ ] En `onFileSelected`: cuando se selecciona un archivo, setear `userRemovedLogo = false` (el usuario eligió nuevo logo).
- [ ] Al hacer clic en “Quitar logo”: setear `userRemovedLogo = true`, limpiar `selectedFile`, `logoPreview`, `logoPath`, `logoBlobUrl` (y revocar URL del blob si existe), y hacer `cdr.detectChanges()` si hace falta.

---

## 3. UpdateCompanyComponent – UI

- [ ] En la sección “Logo de la Compañía”, mostrar botón **“Quitar logo”** (o “Eliminar logo”) cuando haya logo actual o preview: `*ngIf="(logoPreview || logoPath || logoBlobUrl) && edition"`.
- [ ] Estilo: secundario o warn, icono tipo `delete_outline` o `clear`, que no compita con “Seleccionar Logo”.
- [ ] (Opcional) Confirmación corta antes de quitar (ej. “¿Quitar el logo de la compañía?”) o quitar directo y que “Guardar” persista el cambio.

---

## 4. UpdateCompanyComponent – lógica al guardar (edición)

- [ ] En `save()`, cuando `this.edition && this.idCompany != null`:
  - Si **`userRemovedLogo === true`**:
    - Llamar `companyService.deleteLogo(this.idCompany)`.
    - En el callback de éxito: armar el payload del PATCH **sin** `logoPath` (o con `logoPath: null` si el backend lo exige) y llamar `update()` con ese payload.
    - Tras éxito del update: toast de éxito, `dialogRef.close(true)`.
    - Tratamiento de error: toast con mensaje de error (delete o update).
  - Si **`userRemovedLogo === false`** y no hay `selectedFile`: flujo actual (getLogoPath → updateCompanyWithLogoPath con existingLogoPath).
  - Si hay **`selectedFile`**: flujo actual (update y luego uploadLogo).
- [ ] Asegurar que cuando `userRemovedLogo` es true **no** se llame a `getLogoPath` para rellenar el payload (el logo debe quedar “ninguno”).

---

## 5. Actualizar `updateCompanyWithLogoPath` (o equivalente)

- [ ] Permitir que se invoque con `existingLogoPath === null` cuando el usuario quitó el logo (por ejemplo desde el flujo de delete + update).
- [ ] Al armar `this.company` para el PATCH, si el logo fue quitado, enviar `logoPath: undefined` o `logoPath: null` según lo que espere el backend (y que no se sobrescriba con un logo existente).

---

## 6. Pruebas manuales

- [ ] Editar compañía con logo → “Quitar logo” → Guardar → verificar que el logo desaparece (y que en otra edición ya no aparece).
- [ ] Editar compañía sin logo → verificar que no se muestre “Quitar logo” o que no rompa.
- [ ] Editar compañía → Quitar logo → luego elegir nuevo archivo → Guardar → verificar que se sube el nuevo logo (no se llama delete).
- [ ] (Opcional) Compañía sin logo → abrir edición → “Quitar logo” (si el botón se muestra) → Guardar: backend responde 200 idempotente y la compañía sigue sin logo.

---

## 7. (Opcional) Constantes / mensajes

- [ ] Si se muestra un mensaje específico al “quitar logo” (ej. en toast), definir constante tipo `UPDATE.MESSAGE_REMOVE_COMPANY_LOGO` o reutilizar mensaje genérico de actualización.

---

## Orden sugerido

1. CompanyService: `deleteLogo` + test.  
2. UpdateCompanyComponent: estado `userRemovedLogo` y handler “Quitar logo”.  
3. UI: botón “Quitar logo” en el template.  
4. Save: rama `userRemovedLogo` → deleteLogo + PATCH sin logo.  
5. Ajustar `updateCompanyWithLogoPath` (o el payload del PATCH) para el caso “logo quitado”.  
6. Pruebas manuales.
