import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { MenuAdminService } from './menu-admin.service';
import { environment } from '../../../environments/environment';
import {
  MenuAdmin,
  CreateMenuRequest,
  UpdateMenuRequest,
} from '../../_model/admin/menu-admin';

describe('MenuAdminService', () => {
  let service: MenuAdminService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_AUTH}/api/admin/menu`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MenuAdminService],
    });

    service = TestBed.inject(MenuAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAllMenus', () => {
    it('should get all menus', () => {
      const mockResponse: MenuAdmin[] = [
        {
          id: 1,
          name: 'Facturación',
          icon: 'receipt',
          url: '/invoices',
          featureCode: 'INVOICE_CREATE',
          order: 1,
          active: true,
          subMenus: [],
        },
      ];

      service.getAllMenus().subscribe((menus) => {
        expect(menus.length).toBe(1);
        expect(menus[0].name).toBe('Facturación');
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('getMenuById', () => {
    it('should get menu by ID', () => {
      const id = 1;
      const mockResponse: MenuAdmin = {
        id: 1,
        name: 'Facturación',
        icon: 'receipt',
        url: '/invoices',
        featureCode: 'INVOICE_CREATE',
        order: 1,
        active: true,
        subMenus: [],
      };

      service.getMenuById(id).subscribe((menu) => {
        expect(menu.id).toBe(1);
        expect(menu.name).toBe('Facturación');
      });

      const req = httpMock.expectOne(`${baseUrl}/${id}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('createMenu', () => {
    it('should create a new menu', () => {
      const request: CreateMenuRequest = {
        name: 'Nuevo Menú',
        icon: 'menu',
        url: '/new-menu',
        featureCode: 'NEW_MENU',
        parentId: null,
        order: 1,
        active: true,
      };

      const mockResponse: MenuAdmin = {
        id: 2,
        name: 'Nuevo Menú',
        icon: 'menu',
        url: '/new-menu',
        featureCode: 'NEW_MENU',
        order: 1,
        active: true,
        subMenus: [],
      };

      service.createMenu(request).subscribe((menu) => {
        expect(menu.id).toBe(2);
        expect(menu.name).toBe('Nuevo Menú');
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('updateMenu', () => {
    it('should update an existing menu', () => {
      const id = 1;
      const request: UpdateMenuRequest = {
        name: 'Menú Actualizado',
        icon: 'update',
        url: '/updated-menu',
        featureCode: 'UPDATED_MENU',
        order: 2,
        active: true,
      };

      const mockResponse: MenuAdmin = {
        id: 1,
        name: 'Menú Actualizado',
        icon: 'update',
        url: '/updated-menu',
        featureCode: 'UPDATED_MENU',
        order: 2,
        active: true,
        subMenus: [],
      };

      service.updateMenu(id, request).subscribe((menu) => {
        expect(menu.name).toBe('Menú Actualizado');
      });

      const req = httpMock.expectOne(`${baseUrl}/${id}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('deleteMenu', () => {
    it('should delete a menu', () => {
      const id = 1;

      service.deleteMenu(id).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/${id}/hard`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('reorderMenus', () => {
    it('should reorder menus', () => {
      const menuOrders = [
        { id: 1, order: 1 },
        { id: 2, order: 2 },
      ];

      service.reorderMenus(menuOrders).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/reorder`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ menuOrders });
      req.flush(null);
    });
  });

  describe('syncFeaturePermission', () => {
    it('should sync feature permission', () => {
      const featureCode = 'INVOICE_CREATE';
      const featureName = 'Crear Facturas';

      service.syncFeaturePermission(featureCode, featureName).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/sync-feature`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ featureCode, featureName });
      req.flush(null);
    });
  });
});
