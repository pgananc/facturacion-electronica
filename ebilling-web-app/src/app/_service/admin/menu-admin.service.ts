import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  MenuAdmin,
  CreateMenuRequest,
  UpdateMenuRequest,
  MenuListResponse,
} from '../../_model/admin/menu-admin';

/**
 * Servicio para gestión administrativa de menús
 * Solo accesible para usuarios con rol super_admin
 */
@Injectable({
  providedIn: 'root',
})
export class MenuAdminService {
  private readonly baseUrl = `${environment.HOST_AUTH}/api/admin/menu`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Obtiene todos los menús en formato árbol
   * GET /api/admin/menu
   */
  getAllMenus(): Observable<MenuAdmin[]> {
    return this.http.get<MenuAdmin[]>(this.baseUrl);
  }

  /**
   * Obtiene un menú específico por ID
   * GET /api/admin/menu/{id}
   */
  getMenuById(id: number): Observable<MenuAdmin> {
    return this.http.get<MenuAdmin>(`${this.baseUrl}/${id}`);
  }

  /**
   * Crea un nuevo menú
   * POST /api/admin/menu
   */
  createMenu(request: CreateMenuRequest): Observable<MenuAdmin> {
    return this.http.post<MenuAdmin>(this.baseUrl, request);
  }

  /**
   * Actualiza un menú existente
   * PUT /api/admin/menu/{id}
   */
  updateMenu(id: number, request: UpdateMenuRequest): Observable<MenuAdmin> {
    return this.http.put<MenuAdmin>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Elimina un menú físicamente (hard delete)
   * DELETE /api/admin/menu/{id}/hard
   */
  deleteMenu(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/hard`);
  }

  /**
   * Reordena los menús (actualiza el campo order)
   * PUT /api/admin/menu/reorder
   */
  reorderMenus(menuOrders: { id: number; order: number }[]): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/reorder`, { menuOrders });
  }

  /**
   * Crea o actualiza un feature_permission asociado a un menú
   * POST /api/admin/menu/sync-feature
   */
  syncFeaturePermission(
    featureCode: string,
    featureName: string
  ): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/sync-feature`, {
      featureCode,
      featureName,
    });
  }

  /**
   * Verifica si un feature_permission existe
   * GET /api/admin/menu/check-feature/{featureCode}
   * 
   * Si el feature no existe (404/500), retorna { exists: false } en lugar de error
   */
  checkFeatureExists(featureCode: string): Observable<{ exists: boolean }> {
    return this.http.get<{ exists: boolean }>(
      `${this.baseUrl}/check-feature/${featureCode}`
    ).pipe(
      catchError((error) => {
        // Si el feature no existe (404 o 500 con mensaje de "not found"), 
        // esto es normal, no es un error. Retornar { exists: false }
        if (
          error.status === 404 ||
          (error.status === 500 && 
           error.error?.message?.includes('not found') ||
           error.error?.message?.includes('Feature not found'))
        ) {
          return of({ exists: false });
        }
        // Para otros errores, propagar el error
        return throwError(() => error);
      })
    );
  }
}
