# 🔧 InvoiceService con Token Manual (Backup)

## Si JwtModule no agrega el header automáticamente, usa esta versión:

```typescript
import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { InvoiceDto } from "../../_model/invoice/invoice-dto";

@Injectable({ providedIn: "root" })
export class InvoiceService {
  private readonly baseUrl = `${environment.HOST_EBILLING}/api/invoice`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Obtiene los headers con el token JWT desde sessionStorage
   */
  private getAuthHeaders(): HttpHeaders {
    const token = sessionStorage.getItem(environment.TOKEN);

    console.debug("🔐 Agregando token manualmente al header:", {
      tokenExists: !!token,
      tokenLength: token?.length || 0,
    });

    return new HttpHeaders({
      "Content-Type": "application/json",
      Authorization: token ? `Bearer ${token}` : "",
    });
  }

  /**
   * Registra una nueva factura (borrador o para firmar)
   * POST /api/invoice/register
   */
  register(invoice: InvoiceDto): Observable<any> {
    return this.http.post(`${this.baseUrl}/register`, invoice, {
      headers: this.getAuthHeaders(),
    });
  }

  /**
   * Firma y envía una factura al SRI
   * POST /api/invoice/sign-and-send/{idInvoice}
   */
  signAndSend(idInvoice: number): Observable<any> {
    return this.http.post(
      `${this.baseUrl}/sign-and-send/${idInvoice}`,
      {},
      {
        headers: this.getAuthHeaders(),
      }
    );
  }

  /**
   * Obtiene una factura por ID
   * GET /api/invoice/{id}
   */
  findById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${id}`, {
      headers: this.getAuthHeaders(),
    });
  }

  /**
   * Lista todas las facturas (con paginación)
   * GET /api/invoice
   */
  findAll(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get(`${this.baseUrl}?page=${page}&size=${size}`, {
      headers: this.getAuthHeaders(),
    });
  }
}
```

## Aplicar esta solución:

```bash
# Reemplazar el contenido de:
# src/app/_service/invoice/invoice.service.ts
```
