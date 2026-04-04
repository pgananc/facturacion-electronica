/**
 * Modelos para Administración de Menús (super_admin)
 *
 * Este sistema permite gestionar los menús del sistema de forma dinámica,
 * sin necesidad de usar Flyway migrations.
 */

/**
 * Feature Permission asociado a un menú
 */
export interface MenuFeature {
  featureCode: string;
  featureName: string;
  description?: string;
  module?: string;
  allowedRoles?: number[];
  requiredPlans?: string[];
  isActive?: boolean;
}

/**
 * Menú completo con información administrativa
 */
export interface MenuAdmin {
  id: number;
  name: string;
  icon: string;
  url: string | null;
  featureCode?: string | null;
  feature?: MenuFeature | null;
  parentId?: number | null;
  active: boolean;
  order?: number | null;
  subMenus: MenuAdmin[];
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Request para crear un nuevo menú
 */
export interface CreateMenuRequest {
  name: string;
  icon: string;
  url: string | null;
  featureCode?: string | null;
  parentId?: number | null;
  active: boolean;
  order?: number | null;
}

/**
 * Request para actualizar un menú existente
 */
export interface UpdateMenuRequest {
  name?: string;
  icon?: string;
  url?: string | null;
  featureCode?: string | null;
  parentId?: number | null;
  active?: boolean;
  order?: number | null;
}

/**
 * Response del backend al listar menús
 */
export interface MenuListResponse {
  menus: MenuAdmin[];
  total: number;
}
