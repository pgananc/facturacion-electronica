# 📄 Documentación API - Guías de Remisión para Angular

**Versión:** 1.0.0  
**Fecha:** Diciembre 2025  
**Base URL:** `http://localhost:8480/api`

---

## 📋 Tabla de Contenidos

1. [Información General](#información-general)
2. [Endpoints de Guías de Remisión](#endpoints-de-guías-de-remisión)
3. [Endpoints de Transportistas](#endpoints-de-transportistas)
4. [Modelos TypeScript](#modelos-typescript)
5. [Servicios Angular](#servicios-angular)
6. [Flujos de Trabajo](#flujos-de-trabajo)
7. [Ejemplos de Uso](#ejemplos-de-uso)
8. [Manejo de Errores](#manejo-de-errores)

---

## 🌐 Información General

### URL Base

| Ambiente       | URL Base                    |
| -------------- | --------------------------- |
| **Localhost**  | `http://localhost:8480`     |
| **Desarrollo** | `http://85.239.237.88:8480` |

### Headers Comunes

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>  # Si se implementa autenticación
```

### Conceptos Clave

- **Guía de Remisión:** Documento que acompaña el traslado de mercaderías
- **Transportista (Carrier):** Empresa o persona que transporta los productos
- **Borrador:** Guía guardada sin enviar al SRI (puede editarse/eliminarse)
- **Autorizada:** Guía enviada y autorizada por el SRI
- **Destinatario:** Cliente que recibe la mercadería

---

## 🚀 Endpoints de Guías de Remisión

### 1. Listar Todas las Guías

**GET** `/api/guides`

Obtiene la lista completa de todas las guías de remisión con datos del cliente y transportista.

**Request:**

```http
GET http://localhost:8480/api/guides
```

**Response - Success (200):**

```json
[
  {
    "idGuide": 1,
    "idCompany": 1,
    "idClient": 100,
    "idCarrier": 5,
    "guideNumber": "001-001-0000001000",
    "issueDate": "2025-01-28T10:30:00",
    "startDate": "2025-01-28T00:00:00",
    "endDate": "2025-01-29T00:00:00",
    "originAddress": "Av. Origen 123, Quito",
    "destinationAddress": "Av. Destino 456, Guayaquil",
    "transportPlate": "ABC-1234",
    "transportType": "Terrestre",
    "reasonForTransfer": "Venta",
    "route": "Quito - Guayaquil",
    "buyerName": "Cliente Demo S.A.",
    "buyerIdentification": "0987654321",
    "carrierName": "Transportes ABC S.A.",
    "carrierIdentification": "1234567890002",
    "guideStatus": "AUTORIZADO",
    "emailSent": true,
    "emailSentDate": "2025-01-28T10:35:00",
    "creationDate": "2025-01-28T10:30:00"
  }
]
```

---

### 2. Obtener Guía por ID

**GET** `/api/guides/{id}`

Obtiene los datos completos de una guía de remisión, incluyendo detalles de productos y documentos relacionados.

**Request:**

```http
GET http://localhost:8480/api/guides/1
```

**Response - Success (200):**

```json
{
  "idGuide": 1,
  "idCompany": 1,
  "idClient": 100,
  "idCarrier": 5,
  "companyDto": {
    "idCompany": 1,
    "name": "Empresa Test S.A.",
    "nameCommercial": "Empresa Test",
    "identification": "1234567890001"
  },
  "clientDto": {
    "idClient": 100,
    "name": "Cliente Demo S.A.",
    "identification": "0987654321",
    "idType": 2
  },
  "carrierDto": {
    "idCarrier": 5,
    "name": "Transportes ABC S.A.",
    "identification": "1234567890002",
    "identificationType": "04"
  },
  "establishment": {
    "idEstablishment": 1,
    "code": "001",
    "address": "Av. Principal 123",
    "issuancePoint": {
      "idIssuancePoint": 1,
      "code": "001",
      "sequentialInvoice": "1000"
    }
  },
  "issueDate": "2025-01-28",
  "startDate": "2025-01-28",
  "endDate": "2025-01-29",
  "originAddress": "Av. Origen 123, Quito",
  "destinationAddress": "Av. Destino 456, Guayaquil",
  "transportPlate": "ABC-1234",
  "transportType": "Terrestre",
  "reasonForTransfer": "Venta",
  "route": "Quito - Guayaquil",
  "guideDetails": [
    {
      "idGuideDetail": 1,
      "idProduct": 10,
      "productDto": {
        "idProduct": 10,
        "mainCode": "PROD-001",
        "name": "Producto Test",
        "description": "Descripción del producto",
        "unitPrice": 100.0
      },
      "quantity": 10,
      "description": "Producto para transporte"
    }
  ],
  "documentReferences": [
    {
      "idGuideDocumentReference": 1,
      "documentType": "01",
      "documentNumber": "001-001-000000500",
      "accessKey": "28012025123456789012345678901234567890123456789",
      "issueDate": "2025-01-28T10:00:00"
    }
  ],
  "guideNumber": "001-001-0000001000",
  "sequential": 1000,
  "accessKey": "28012025123456789012345678901234567890123456789",
  "authorizationNumber": "1234567890",
  "authorizationDate": "2025-01-28T10:35:00",
  "status": "AUTORIZADO"
}
```

**Response - Not Found (404):**

```json
{
  "status": 404,
  "message": "Guía no encontrada"
}
```

---

### 3. Crear Guía de Remisión

**POST** `/api/guides`

Crea una nueva guía de remisión y la envía automáticamente al SRI. Si la autorización es exitosa, se genera el PDF y se envía por email al cliente.

**Request:**

```json
{
  "companyDto": {
    "idCompany": 1,
    "name": "Empresa Test S.A.",
    "nameCommercial": "Empresa Test",
    "identification": "1234567890001"
  },
  "clientDto": {
    "idClient": 100,
    "name": "Cliente Demo S.A.",
    "identification": "0987654321",
    "idType": 2,
    "contacts": [
      {
        "contactType": "MAI",
        "value": "cliente@demo.com",
        "status": true
      }
    ]
  },
  "carrierDto": {
    "idCarrier": 5,
    "name": "Transportes ABC S.A.",
    "identification": "1234567890002",
    "identificationType": "04"
  },
  "establishment": {
    "idEstablishment": 1,
    "code": "001",
    "address": "Av. Principal 123",
    "issuancePoint": {
      "idIssuancePoint": 1,
      "code": "001"
    }
  },
  "issueDate": "2025-01-28",
  "startDate": "2025-01-28",
  "endDate": "2025-01-29",
  "originAddress": "Av. Origen 123, Quito",
  "destinationAddress": "Av. Destino 456, Guayaquil",
  "transportPlate": "ABC-1234",
  "transportType": "Terrestre",
  "reasonForTransfer": "Venta",
  "route": "Quito - Guayaquil",
  "additionalInformation": "Manejar con cuidado",
  "guideDetails": [
    {
      "idProduct": 10,
      "quantity": 10,
      "description": "Producto para transporte"
    }
  ],
  "documentReferences": [
    {
      "documentType": "01",
      "documentNumber": "001-001-000000500",
      "accessKey": "28012025123456789012345678901234567890123456789",
      "issueDate": "2025-01-28T10:00:00"
    }
  ]
}
```

**Response - Success (200):**

```json
{
  "code": 200,
  "status": "OK",
  "message": "Guía de remisión creada y autorizada exitosamente",
  "guideDto": {
    "idGuide": 1,
    "guideNumber": "001-001-0000001000",
    "accessKey": "28012025123456789012345678901234567890123456789",
    "authorizationNumber": "1234567890",
    "authorizationDate": "2025-01-28T10:35:00",
    "authorizationStatusSri": "AUTORIZADO"
  }
}
```

**Response - Error (400):**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "El transportista es obligatorio"
}
```

---

### 4. Guardar Guía como Borrador

**POST** `/api/guides/draft`

Guarda una guía de remisión sin enviarla al SRI. Puede editarse o eliminarse posteriormente.

**Request:** (Igual que crear guía)

```http
POST http://localhost:8480/api/guides/draft
Content-Type: application/json

{
  "companyDto": { ... },
  "clientDto": { ... },
  "carrierDto": { ... },
  ...
}
```

**Response - Success (200):**

```json
{
  "code": 200,
  "status": "OK",
  "message": "Guía de remisión guardada como borrador",
  "guideDto": {
    "idGuide": 1,
    "guideNumber": null,
    "status": "BORRADOR"
  }
}
```

---

### 5. Actualizar Guía (Solo Borradores)

**PUT** `/api/guides/{id}`

Actualiza una guía de remisión que aún no ha sido enviada al SRI.

**Request:**

```http
PUT http://localhost:8480/api/guides/1
Content-Type: application/json

{
  "companyDto": { ... },
  "clientDto": { ... },
  ...
}
```

**Response - Success (200):**

```json
{
  "code": 200,
  "status": "OK",
  "guideDto": { ... }
}
```

**Response - Error (400):**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "No se puede actualizar una guía autorizada"
}
```

---

### 6. Eliminar Guía (Solo Borradores)

**DELETE** `/api/guides/{id}`

Elimina una guía de remisión que aún no ha sido enviada al SRI.

**Request:**

```http
DELETE http://localhost:8480/api/guides/1
```

**Response - Success (204):** No Content

**Response - Error (400):**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "No se puede eliminar una guía autorizada"
}
```

---

### 7. Enviar Borrador al SRI

**POST** `/api/guides/{id}/send-to-sri?password={password}`

Envía un borrador de guía de remisión al SRI para su autorización.

**Request:**

```http
POST http://localhost:8480/api/guides/1/send-to-sri?password=miPassword123
```

**Response - Success (200):**

```json
{
  "code": 200,
  "status": "OK",
  "message": "Guía enviada al SRI exitosamente",
  "guideDto": {
    "idGuide": 1,
    "guideNumber": "001-001-0000001000",
    "authorizationStatusSri": "AUTORIZADO"
  }
}
```

---

### 8. Buscar Guías con Filtros

**GET** `/api/guides/search`

Busca guías de remisión con filtros opcionales y paginación.

**Request:**

```http
GET http://localhost:8480/api/guides/search?guideNumber=001-001-0000001000&idClient=100&page=0&size=10&sort=creationDate,desc
```

**Parámetros de Query:**

| Parámetro      | Tipo    | Requerido | Descripción                    |
| -------------- | ------- | --------- | ----------------------------- |
| `guideNumber`  | String  | No        | Número de guía                |
| `idClient`     | Long    | No        | ID del cliente destinatario   |
| `idCarrier`    | Long    | No        | ID del transportista          |
| `page`         | Integer | No        | Número de página (default: 0)  |
| `size`         | Integer | No        | Tamaño de página (default: 20)|
| `sort`         | String  | No        | Campo y dirección (ej: `creationDate,desc`) |

**Response - Success (200):**

```json
{
  "content": [
    {
      "idGuide": 1,
      "guideNumber": "001-001-0000001000",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 45,
  "totalPages": 5,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false
}
```

---

### 9. Descargar PDF

**GET** `/api/guides/{id}/pdf`

Descarga el archivo PDF de la guía de remisión autorizada.

**Request:**

```http
GET http://localhost:8480/api/guides/1/pdf
```

**Response - Success (200):**

- **Content-Type:** `application/pdf`
- **Content-Disposition:** `attachment; filename="GR-{clave_acceso}.pdf"`
- **Body:** Archivo PDF binario

**Response - Not Found (404):** Si la guía no existe o no tiene PDF generado

---

### 10. Descargar XML

**GET** `/api/guides/{id}/xml`

Descarga el archivo XML autorizado del SRI.

**Request:**

```http
GET http://localhost:8480/api/guides/1/xml
```

**Response - Success (200):**

- **Content-Type:** `application/xml`
- **Content-Disposition:** `attachment; filename="{clave_acceso}.xml"`
- **Body:** Archivo XML binario

---

### 11. Enviar Guía por Email

**POST** `/api/guides/{id}/send-email?email={email}`

Envía la guía de remisión por email al cliente. Si no se proporciona email, se usa el email del cliente registrado.

**Request:**

```http
POST http://localhost:8480/api/guides/1/send-email?email=cliente@alternativo.com
```

**Response - Success (200):** No Content

**Response - Error (400):**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "No se encontró email del cliente"
}
```

---

## 🚚 Endpoints de Transportistas

### 1. Listar Transportistas

**GET** `/api/carrier`

Obtiene la lista de transportistas de una empresa con paginación.

**Request:**

```http
GET http://localhost:8480/api/carrier?idCompany=1&activeOnly=true&page=0&size=10&sort=name,asc
```

**Parámetros de Query:**

| Parámetro     | Tipo    | Requerido | Descripción                          |
| ------------- | ------- | --------- | ------------------------------------ |
| `idCompany`   | Long    | **Sí**    | ID de la empresa                     |
| `activeOnly`  | Boolean | No        | Solo activos (default: false)        |
| `page`        | Integer | No        | Número de página (default: 0)         |
| `size`        | Integer | No        | Tamaño de página (default: 10)       |
| `sort`        | String  | No        | Campo y dirección (ej: `name,asc`)   |

**Response - Success (200):**

```json
{
  "content": [
    {
      "idCarrier": 1,
      "idCompany": 1,
      "name": "Transportes ABC S.A.",
      "identification": "1234567890002",
      "identificationType": "04",
      "address": "Av. Transporte 789",
      "phone": "0999999999",
      "email": "contacto@transportesabc.com",
      "licenseNumber": "LIC-12345",
      "vehicleType": "Camión",
      "status": true
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 25,
  "totalPages": 3
}
```

---

### 2. Obtener Transportista por ID

**GET** `/api/carrier/{id}`

**Request:**

```http
GET http://localhost:8480/api/carrier/1
```

**Response - Success (200):**

```json
{
  "idCarrier": 1,
  "idCompany": 1,
  "name": "Transportes ABC S.A.",
  "identification": "1234567890002",
  "identificationType": "04",
  "address": "Av. Transporte 789",
  "phone": "0999999999",
  "email": "contacto@transportesabc.com",
  "licenseNumber": "LIC-12345",
  "vehicleType": "Camión",
  "status": true
}
```

---

### 3. Crear Transportista

**POST** `/api/carrier`

**Request:**

```json
{
  "idCompany": 1,
  "name": "Transportes XYZ S.A.",
  "identification": "9876543210001",
  "identificationType": "04",
  "address": "Av. Nueva 456",
  "phone": "0988888888",
  "email": "contacto@transportesxyz.com",
  "licenseNumber": "LIC-67890",
  "vehicleType": "Furgoneta"
}
```

**Response - Success (201):**

```json
{
  "idCarrier": 2,
  "idCompany": 1,
  "name": "Transportes XYZ S.A.",
  "identification": "9876543210001",
  "identificationType": "04",
  "address": "Av. Nueva 456",
  "phone": "0988888888",
  "email": "contacto@transportesxyz.com",
  "licenseNumber": "LIC-67890",
  "vehicleType": "Furgoneta",
  "status": true
}
```

**Response - Error (400):**

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "Ya existe un transportista con la identificación 9876543210001 para esta empresa"
}
```

---

### 4. Actualizar Transportista

**PUT** `/api/carrier/{id}`

**Request:**

```http
PUT http://localhost:8480/api/carrier/1
Content-Type: application/json

{
  "name": "Transportes ABC S.A. Actualizado",
  "phone": "0999999999",
  "email": "nuevo@transportesabc.com"
}
```

**Response - Success (200):**

```json
{
  "idCarrier": 1,
  "name": "Transportes ABC S.A. Actualizado",
  ...
}
```

---

### 5. Eliminar Transportista

**DELETE** `/api/carrier/{id}`

Desactiva un transportista (soft delete).

**Request:**

```http
DELETE http://localhost:8480/api/carrier/1
```

**Response - Success (204):** No Content

---

## 📦 Modelos TypeScript

### GuideDto

```typescript
export interface GuideDto {
  // Campos generados automáticamente (NO enviar en request)
  idGuide?: number;
  idCompany?: number;
  idClient?: number;
  idEstablishment?: number;
  idIssuancePoint?: number;
  sequential?: number;
  creationDate?: string; // ISO 8601
  accessKey?: string;
  guideNumber?: string;
  authorizationNumber?: string;
  authorizationDate?: string; // ISO 8601
  status?: string;
  environment?: string;
  emailSent?: boolean;
  emailSentDate?: string; // ISO 8601

  // Campos obligatorios
  companyDto: CompanyDto;
  clientDto: ClientDto;
  carrierDto: CarrierDto;
  issueDate: string; // yyyy-MM-dd
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
  originAddress: string;
  destinationAddress: string;
  guideDetails: GuideDetailDto[]; // Mínimo 1

  // Campos opcionales
  establishment?: EstablishmentDto;
  transportType?: string;
  transportPlate?: string;
  reasonForTransfer?: string;
  route?: string;
  additionalInformation?: string;
  documentReferences?: GuideDocumentReferenceDto[];
  idCarrier?: number; // Si el transportista ya existe
}

export interface GuideDetailDto {
  idGuideDetail?: number;
  idGuide?: number;
  idProduct: number;
  productDto?: ProductDto;
  quantity: number;
  description?: string;
  unit?: string;
  weight?: number;
  additionalInfo?: string;
}

export interface GuideDocumentReferenceDto {
  idGuideDocumentReference?: number;
  documentType: string; // "01" = Factura, "04" = Nota de Crédito
  documentNumber: string;
  accessKey?: string;
  issueDate?: string; // ISO 8601
}

export interface GuideListResponseDto {
  idGuide: number;
  idCompany: number;
  idClient: number;
  idCarrier: number;
  guideNumber: string;
  issueDate: string;
  startDate: string;
  endDate: string;
  originAddress: string;
  destinationAddress: string;
  transportPlate: string;
  transportType: string;
  reasonForTransfer: string;
  route?: string;
  buyerName: string;
  buyerIdentification: string;
  carrierName: string;
  carrierIdentification: string;
  guideStatus: string; // "BORRADOR", "AUTORIZADO", "NO_AUTORIZADO"
  emailSent: boolean;
  emailSentDate?: string;
  creationDate: string;
}

export interface GuideResponse {
  code: number;
  status: string;
  message?: string;
  guideDto?: GuideDto;
  guideDtos?: GuideDto[];
  idGuide?: number;
  guideNumber?: string;
  accessKey?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  receptionStatusSri?: string;
  authorizationStatusSri?: string;
  storagePath?: string;
}
```

### CarrierDto

```typescript
export interface CarrierDto {
  idCarrier?: number;
  idCompany: number;
  identificationType: string; // "04" = RUC, "05" = Cédula, "07" = Consumidor Final
  identification: string;
  name: string;
  address?: string;
  phone?: string;
  email?: string;
  licenseNumber?: string;
  vehicleType?: string;
  status?: boolean;
}
```

### DTOs Compartidos

```typescript
export interface CompanyDto {
  idCompany: number;
  name: string;
  nameCommercial?: string;
  identification: string;
  logoPath?: string;
  regime?: string;
  specialTaxpayerResolution?: string;
}

export interface ClientDto {
  idClient: number;
  name: string;
  identification: string;
  idType: number; // 1=Cédula, 2=RUC, 3=Pasaporte, 4=Consumidor Final, 5=Identificación Exterior
  contacts?: ContactDto[];
}

export interface ContactDto {
  contactType: string; // "MAI" = Email, "PHO" = Teléfono, "ADD" = Dirección
  value: string;
  status?: boolean;
}

export interface EstablishmentDto {
  idEstablishment: number;
  code: string;
  address: string;
  type?: string; // "MATRIZ", "SUCURSAL"
  parentAddress?: string;
  issuancePoint: IssuancePointDto;
}

export interface IssuancePointDto {
  idIssuancePoint: number;
  code: string;
  sequentialInvoice?: string;
}

export interface ProductDto {
  idProduct: number;
  mainCode: string;
  auxiliarCode?: string;
  name: string;
  description?: string;
  unitPrice: number;
  rate?: number;
  iceRate?: number;
}
```

---

## 🔧 Servicios Angular

### guide.service.ts

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  GuideDto,
  GuideResponse,
  GuideListResponseDto,
} from '../models/guide.model';
import { Page } from '../models/page.model';

@Injectable({
  providedIn: 'root',
})
export class GuideService {
  private apiUrl = 'http://localhost:8480/api/guides';

  constructor(private http: HttpClient) {}

  /**
   * Lista todas las guías de remisión
   */
  getAllGuides(): Observable<GuideListResponseDto[]> {
    return this.http.get<GuideListResponseDto[]>(this.apiUrl);
  }

  /**
   * Obtiene una guía por ID
   */
  getGuideById(id: number): Observable<GuideDto> {
    return this.http.get<GuideDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea una nueva guía y la envía al SRI
   */
  createGuide(guide: GuideDto): Observable<GuideResponse> {
    return this.http.post<GuideResponse>(this.apiUrl, guide);
  }

  /**
   * Guarda una guía como borrador
   */
  saveDraft(guide: GuideDto): Observable<GuideResponse> {
    return this.http.post<GuideResponse>(`${this.apiUrl}/draft`, guide);
  }

  /**
   * Actualiza una guía (solo borradores)
   */
  updateGuide(id: number, guide: GuideDto): Observable<GuideResponse> {
    return this.http.put<GuideResponse>(`${this.apiUrl}/${id}`, guide);
  }

  /**
   * Elimina una guía (solo borradores)
   */
  deleteGuide(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Envía un borrador al SRI
   */
  sendToSri(id: number, password: string): Observable<GuideResponse> {
    const params = new HttpParams().set('password', password);
    return this.http.post<GuideResponse>(
      `${this.apiUrl}/${id}/send-to-sri`,
      null,
      { params }
    );
  }

  /**
   * Busca guías con filtros
   */
  searchGuides(
    filters: {
      guideNumber?: string;
      idClient?: number;
      idCarrier?: number;
    },
    page: number = 0,
    size: number = 20,
    sort: string = 'creationDate,desc'
  ): Observable<Page<any>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filters.guideNumber) {
      params = params.set('guideNumber', filters.guideNumber);
    }
    if (filters.idClient) {
      params = params.set('idClient', filters.idClient.toString());
    }
    if (filters.idCarrier) {
      params = params.set('idCarrier', filters.idCarrier.toString());
    }

    return this.http.get<Page<any>>(`${this.apiUrl}/search`, { params });
  }

  /**
   * Descarga el PDF de una guía
   */
  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, {
      responseType: 'blob',
    });
  }

  /**
   * Descarga el XML de una guía
   */
  downloadXml(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/xml`, {
      responseType: 'blob',
    });
  }

  /**
   * Envía la guía por email
   */
  sendByEmail(id: number, email?: string): Observable<void> {
    let params = new HttpParams();
    if (email) {
      params = params.set('email', email);
    }
    return this.http.post<void>(`${this.apiUrl}/${id}/send-email`, null, {
      params,
    });
  }
}
```

### carrier.service.ts

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CarrierDto } from '../models/carrier.model';
import { Page } from '../models/page.model';

@Injectable({
  providedIn: 'root',
})
export class CarrierService {
  private apiUrl = 'http://localhost:8480/api/carrier';

  constructor(private http: HttpClient) {}

  /**
   * Lista transportistas de una empresa
   */
  getCarriers(
    idCompany: number,
    activeOnly: boolean = false,
    page: number = 0,
    size: number = 10,
    sort: string = 'name,asc'
  ): Observable<Page<CarrierDto>> {
    let params = new HttpParams()
      .set('idCompany', idCompany.toString())
      .set('activeOnly', activeOnly.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<Page<CarrierDto>>(this.apiUrl, { params });
  }

  /**
   * Obtiene un transportista por ID
   */
  getCarrierById(id: number): Observable<CarrierDto> {
    return this.http.get<CarrierDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea un nuevo transportista
   */
  createCarrier(carrier: CarrierDto): Observable<CarrierDto> {
    return this.http.post<CarrierDto>(this.apiUrl, carrier);
  }

  /**
   * Actualiza un transportista
   */
  updateCarrier(id: number, carrier: CarrierDto): Observable<CarrierDto> {
    return this.http.put<CarrierDto>(`${this.apiUrl}/${id}`, carrier);
  }

  /**
   * Elimina (desactiva) un transportista
   */
  deleteCarrier(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
```

### page.model.ts

```typescript
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}
```

---

## 🔄 Flujos de Trabajo

### Flujo 1: Crear Guía y Enviar al SRI

```typescript
// 1. Crear guía (se envía automáticamente al SRI)
const guideData: GuideDto = {
  companyDto: { idCompany: 1, name: '...', identification: '...' },
  clientDto: { idClient: 100, name: '...', identification: '...' },
  carrierDto: { idCarrier: 5, name: '...', identification: '...' },
  establishment: { ... },
  issueDate: '2025-01-28',
  startDate: '2025-01-28',
  endDate: '2025-01-29',
  originAddress: 'Av. Origen 123',
  destinationAddress: 'Av. Destino 456',
  guideDetails: [
    {
      idProduct: 10,
      quantity: 10,
      description: 'Producto para transporte',
    },
  ],
};

this.guideService.createGuide(guideData).subscribe({
  next: (response) => {
    if (response.code === 200) {
      console.log('Guía creada:', response.guideDto?.guideNumber);
      // El PDF y email se generan automáticamente si está autorizada
    } else {
      console.error('Error:', response.message);
    }
  },
  error: (error) => {
    console.error('Error al crear guía:', error);
  },
});
```

### Flujo 2: Guardar Borrador y Enviar Después

```typescript
// 1. Guardar como borrador
this.guideService.saveDraft(guideData).subscribe({
  next: (response) => {
    const guideId = response.guideDto?.idGuide;
    console.log('Borrador guardado:', guideId);

    // 2. Enviar al SRI cuando el usuario lo solicite
    this.sendDraftToSri(guideId!);
  },
});

// 3. Enviar borrador al SRI
sendDraftToSri(guideId: number) {
  const password = prompt('Ingrese la contraseña de la firma digital:');
  if (password) {
    this.guideService.sendToSri(guideId, password).subscribe({
      next: (response) => {
        if (response.code === 200) {
          console.log('Guía autorizada:', response.authorizationNumber);
        }
      },
    });
  }
}
```

### Flujo 3: Listar y Filtrar Guías

```typescript
// Listar todas
this.guideService.getAllGuides().subscribe({
  next: (guides) => {
    console.log('Total guías:', guides.length);
  },
});

// Buscar con filtros
this.guideService
  .searchGuides(
    {
      idClient: 100,
      guideNumber: '001-001',
    },
    0,
    20,
    'creationDate,desc'
  )
  .subscribe({
    next: (page) => {
      console.log('Guías encontradas:', page.content);
      console.log('Total:', page.totalElements);
    },
  });
```

### Flujo 4: Descargar PDF y XML

```typescript
downloadGuidePdf(guideId: number) {
  this.guideService.downloadPdf(guideId).subscribe({
    next: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `guia-remision-${guideId}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    },
    error: (error) => {
      console.error('Error al descargar PDF:', error);
    },
  });
}

downloadGuideXml(guideId: number) {
  this.guideService.downloadXml(guideId).subscribe({
    next: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `guia-remision-${guideId}.xml`;
      link.click();
      window.URL.revokeObjectURL(url);
    },
  });
}
```

### Flujo 5: Gestionar Transportistas

```typescript
// Listar transportistas activos
this.carrierService
  .getCarriers(1, true, 0, 10, 'name,asc')
  .subscribe({
    next: (page) => {
      console.log('Transportistas:', page.content);
    },
  });

// Crear nuevo transportista
const newCarrier: CarrierDto = {
  idCompany: 1,
  name: 'Transportes Nuevos S.A.',
  identification: '1234567890003',
  identificationType: '04',
  address: 'Av. Nueva 789',
  phone: '0999999999',
  email: 'contacto@nuevos.com',
};

this.carrierService.createCarrier(newCarrier).subscribe({
  next: (carrier) => {
    console.log('Transportista creado:', carrier.idCarrier);
  },
  error: (error) => {
    if (error.status === 400) {
      console.error('Error:', error.error.message);
    }
  },
});
```

---

## ⚠️ Manejo de Errores

### Ejemplo de Manejo de Errores

```typescript
import { catchError, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

createGuide(guide: GuideDto): Observable<GuideResponse> {
  return this.http.post<GuideResponse>(this.apiUrl, guide).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 400) {
        const errorMessage = error.error?.message || 'Error de validación';
        console.error('Error de validación:', errorMessage);
        return throwError(() => new Error(errorMessage));
      } else if (error.status === 500) {
        console.error('Error del servidor:', error.message);
        return throwError(() => new Error('Error del servidor'));
      }
      return throwError(() => new Error('Error desconocido'));
    })
  );
}
```

### Códigos de Respuesta HTTP

| Código | Significado                    | Acción Recomendada                    |
| ------ | ------------------------------ | ------------------------------------- |
| 200    | OK                             | Operación exitosa                     |
| 201    | Created                        | Recurso creado exitosamente           |
| 204    | No Content                     | Operación exitosa sin contenido       |
| 400    | Bad Request                    | Validar datos enviados                |
| 404    | Not Found                      | Recurso no encontrado                 |
| 500    | Internal Server Error          | Reintentar o contactar soporte        |

---

## 📝 Validaciones Importantes

### Campos Obligatorios para Crear Guía

- ✅ `companyDto` (con `idCompany`, `name`, `identification`)
- ✅ `clientDto` (con `idClient`, `name`, `identification`, `idType`)
- ✅ `carrierDto` (con `name`, `identification`, `identificationType`) **O** `idCarrier`
- ✅ `establishment` (con `idEstablishment`, `code`, `issuancePoint`)
- ✅ `issueDate` (formato: `yyyy-MM-dd`)
- ✅ `startDate` (formato: `yyyy-MM-dd`)
- ✅ `endDate` (formato: `yyyy-MM-dd`)
- ✅ `originAddress` (máximo 300 caracteres)
- ✅ `destinationAddress` (máximo 300 caracteres)
- ✅ `guideDetails` (mínimo 1 producto)

### Validaciones de Negocio

1. **Transportista:** Debe existir en el catálogo o enviarse completo en `carrierDto`
2. **Fechas:** `endDate` debe ser mayor o igual a `startDate`
3. **Productos:** Cada `guideDetail` debe tener `idProduct` y `quantity > 0`
4. **Establecimiento:** Debe tener `issuancePoint` configurado

---

## 🎯 Ejemplos Completos de Uso

### Componente Angular Completo

```typescript
import { Component, OnInit } from '@angular/core';
import { GuideService } from './services/guide.service';
import { CarrierService } from './services/carrier.service';
import { GuideDto, GuideResponse } from './models/guide.model';
import { CarrierDto } from './models/carrier.model';

@Component({
  selector: 'app-guide-form',
  templateUrl: './guide-form.component.html',
})
export class GuideFormComponent implements OnInit {
  guide: GuideDto = {} as GuideDto;
  carriers: CarrierDto[] = [];
  loading = false;
  errorMessage = '';

  constructor(
    private guideService: GuideService,
    private carrierService: CarrierService
  ) {}

  ngOnInit() {
    this.loadCarriers();
    this.initializeGuide();
  }

  loadCarriers() {
    this.carrierService.getCarriers(1, true).subscribe({
      next: (page) => {
        this.carriers = page.content;
      },
      error: (error) => {
        console.error('Error al cargar transportistas:', error);
      },
    });
  }

  initializeGuide() {
    this.guide = {
      companyDto: {
        idCompany: 1,
        name: 'Mi Empresa S.A.',
        identification: '1234567890001',
      },
      clientDto: {
        idClient: 100,
        name: 'Cliente Demo',
        identification: '0987654321',
        idType: 5,
        contacts: [
          {
            contactType: 'MAI',
            value: 'cliente@demo.com',
            status: true,
          },
        ],
      },
      establishment: {
        idEstablishment: 1,
        code: '001',
        address: 'Av. Principal 123',
        issuancePoint: {
          idIssuancePoint: 1,
          code: '001',
        },
      },
      issueDate: new Date().toISOString().split('T')[0],
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
      originAddress: '',
      destinationAddress: '',
      guideDetails: [],
    };
  }

  addProductDetail() {
    if (!this.guide.guideDetails) {
      this.guide.guideDetails = [];
    }
    this.guide.guideDetails.push({
      idProduct: 0,
      quantity: 1,
      description: '',
    });
  }

  removeProductDetail(index: number) {
    this.guide.guideDetails?.splice(index, 1);
  }

  selectCarrier(carrierId: number) {
    this.guide.idCarrier = carrierId;
    this.guide.carrierDto = undefined;
  }

  createGuide() {
    if (!this.validateGuide()) {
      this.errorMessage = 'Por favor complete todos los campos obligatorios';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.guideService.createGuide(this.guide).subscribe({
      next: (response: GuideResponse) => {
        this.loading = false;
        if (response.code === 200) {
          alert(
            `Guía creada exitosamente: ${response.guideDto?.guideNumber}`
          );
          // Redirigir o limpiar formulario
          this.initializeGuide();
        } else {
          this.errorMessage = response.message || 'Error al crear guía';
        }
      },
      error: (error) => {
        this.loading = false;
        if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else {
          this.errorMessage = 'Error al crear guía. Intente nuevamente.';
        }
      },
    });
  }

  saveDraft() {
    this.loading = true;
    this.guideService.saveDraft(this.guide).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.code === 200) {
          alert('Borrador guardado exitosamente');
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Error al guardar borrador';
      },
    });
  }

  validateGuide(): boolean {
    return !!(
      this.guide.companyDto &&
      this.guide.clientDto &&
      (this.guide.carrierDto || this.guide.idCarrier) &&
      this.guide.establishment &&
      this.guide.issueDate &&
      this.guide.startDate &&
      this.guide.endDate &&
      this.guide.originAddress &&
      this.guide.destinationAddress &&
      this.guide.guideDetails &&
      this.guide.guideDetails.length > 0
    );
  }
}
```

---

## 🔍 Búsqueda y Filtrado

### Ejemplo de Búsqueda Avanzada

```typescript
searchGuides() {
  const filters = {
    guideNumber: this.searchForm.value.guideNumber,
    idClient: this.searchForm.value.idClient,
    idCarrier: this.searchForm.value.idCarrier,
  };

  this.guideService
    .searchGuides(filters, this.currentPage, this.pageSize, 'creationDate,desc')
    .subscribe({
      next: (page) => {
        this.guides = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
      },
    });
}
```

---

## 📧 Envío de Emails

### Enviar Guía por Email

```typescript
sendGuideByEmail(guideId: number, email?: string) {
  this.guideService.sendByEmail(guideId, email).subscribe({
    next: () => {
      alert('Guía enviada por email exitosamente');
    },
    error: (error) => {
      if (error.status === 400) {
        alert('No se encontró email del cliente');
      } else {
        alert('Error al enviar email');
      }
    },
  });
}
```

---

## 🎨 Ejemplo de Template HTML

```html
<form [formGroup]="guideForm" (ngSubmit)="createGuide()">
  <!-- Cliente -->
  <div class="form-group">
    <label>Cliente Destinatario *</label>
    <select formControlName="idClient" (change)="onClientChange($event)">
      <option value="">Seleccione...</option>
      <option *ngFor="let client of clients" [value]="client.idClient">
        {{ client.name }} - {{ client.identification }}
      </option>
    </select>
  </div>

  <!-- Transportista -->
  <div class="form-group">
    <label>Transportista *</label>
    <select formControlName="idCarrier" (change)="onCarrierChange($event)">
      <option value="">Seleccione...</option>
      <option *ngFor="let carrier of carriers" [value]="carrier.idCarrier">
        {{ carrier.name }} - {{ carrier.identification }}
      </option>
    </select>
    <button type="button" (click)="openNewCarrierModal()">
      + Nuevo Transportista
    </button>
  </div>

  <!-- Fechas -->
  <div class="form-row">
    <div class="form-group">
      <label>Fecha de Emisión *</label>
      <input type="date" formControlName="issueDate" />
    </div>
    <div class="form-group">
      <label>Fecha Inicio Traslado *</label>
      <input type="date" formControlName="startDate" />
    </div>
    <div class="form-group">
      <label>Fecha Fin Traslado *</label>
      <input type="date" formControlName="endDate" />
    </div>
  </div>

  <!-- Direcciones -->
  <div class="form-group">
    <label>Dirección de Partida *</label>
    <textarea formControlName="originAddress" rows="2"></textarea>
  </div>
  <div class="form-group">
    <label>Dirección de Destino *</label>
    <textarea formControlName="destinationAddress" rows="2"></textarea>
  </div>

  <!-- Placa -->
  <div class="form-group">
    <label>Placa del Vehículo</label>
    <input type="text" formControlName="transportPlate" />
  </div>

  <!-- Motivo -->
  <div class="form-group">
    <label>Motivo del Traslado</label>
    <input type="text" formControlName="reasonForTransfer" />
  </div>

  <!-- Productos -->
  <div class="form-group">
    <label>Productos a Transportar *</label>
    <div
      *ngFor="let detail of guideDetails.controls; let i = index"
      class="product-detail"
    >
      <select [formControl]="detail.get('idProduct')">
        <option value="">Seleccione producto...</option>
        <option *ngFor="let product of products" [value]="product.idProduct">
          {{ product.name }}
        </option>
      </select>
      <input
        type="number"
        [formControl]="detail.get('quantity')"
        placeholder="Cantidad"
        min="1"
      />
      <textarea
        [formControl]="detail.get('description')"
        placeholder="Descripción"
      ></textarea>
      <button type="button" (click)="removeProductDetail(i)">Eliminar</button>
    </div>
    <button type="button" (click)="addProductDetail()">
      + Agregar Producto
    </button>
  </div>

  <!-- Botones -->
  <div class="form-actions">
    <button type="button" (click)="saveDraft()" [disabled]="loading">
      Guardar Borrador
    </button>
    <button type="submit" [disabled]="loading || !guideForm.valid">
      {{ loading ? 'Creando...' : 'Crear y Enviar al SRI' }}
    </button>
  </div>

  <div *ngIf="errorMessage" class="error-message">
    {{ errorMessage }}
  </div>
</form>
```

---

## 📊 Estados de Guía

| Estado          | Descripción                           | Acciones Permitidas                    |
| --------------- | ------------------------------------- | -------------------------------------- |
| `BORRADOR`      | Guía guardada sin enviar al SRI       | Editar, Eliminar, Enviar al SRI        |
| `AUTORIZADO`    | Guía autorizada por el SRI            | Ver, Descargar PDF/XML, Enviar Email   |
| `NO_AUTORIZADO` | Guía rechazada por el SRI             | Ver, Revisar errores                   |
| `EN_PROCESO`    | Guía enviada al SRI, esperando respuesta | Ver, Esperar respuesta                |

---

## 🐛 Errores Comunes y Soluciones

### Error: "El transportista es obligatorio"

**Causa:** No se envió `carrierDto` ni `idCarrier`

**Solución:**

```typescript
// Opción 1: Enviar transportista completo
guide.carrierDto = {
  idCompany: 1,
  name: 'Transportes ABC',
  identification: '1234567890002',
  identificationType: '04',
};

// Opción 2: Usar ID de transportista existente
guide.idCarrier = 5;
```

### Error: "La guía debe tener al menos un detalle"

**Causa:** `guideDetails` está vacío o es null

**Solución:**

```typescript
guide.guideDetails = [
  {
    idProduct: 10,
    quantity: 1,
    description: 'Producto a transportar',
  },
];
```

### Error: "No se puede actualizar una guía autorizada"

**Causa:** Se intenta editar una guía que ya fue enviada al SRI

**Solución:** Solo se pueden editar borradores. Crear una nueva guía si es necesario.

---

## 📚 Recursos Adicionales

- **Base URL:** `http://localhost:8480`
- **Swagger/OpenAPI:** (Si está configurado)
- **Documentación SRI:** [https://www.sri.gob.ec](https://www.sri.gob.ec)

---

## ✅ Checklist de Implementación

- [ ] Configurar URL base en `environment.ts`
- [ ] Crear modelos TypeScript (`GuideDto`, `CarrierDto`, etc.)
- [ ] Crear servicios Angular (`GuideService`, `CarrierService`)
- [ ] Implementar componentes de formulario
- [ ] Implementar listado con paginación
- [ ] Implementar búsqueda y filtros
- [ ] Implementar descarga de PDF/XML
- [ ] Implementar envío por email
- [ ] Manejo de errores y validaciones
- [ ] Pruebas unitarias de servicios
- [ ] Pruebas de integración E2E

---

**Última actualización:** Diciembre 2025  
**Versión API:** 1.0.0

