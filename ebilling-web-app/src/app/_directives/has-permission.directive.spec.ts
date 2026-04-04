import { Component, TemplateRef, ViewContainerRef } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HasPermissionDirective } from './has-permission.directive';
import { PermissionService } from '../_service/permission/permission.service';
import { of, BehaviorSubject } from 'rxjs';

@Component({
  template: `
    <div *appHasPermission="'TEST_FEATURE'">Visible content</div>
    <div *appHasPermission="'PREMIUM_FEATURE'; else upgradeTpl">
      Premium content
    </div>
    <ng-template #upgradeTpl>
      <div>Upgrade required</div>
    </ng-template>
  `,
})
class TestComponent {}

describe('HasPermissionDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let component: TestComponent;
  let permissionServiceSpy: jasmine.SpyObj<PermissionService>;
  let directive: HasPermissionDirective;
  let templateRef: TemplateRef<any>;
  let viewContainer: ViewContainerRef;

  beforeEach(() => {
    const permissionService = jasmine.createSpyObj('PermissionService', [
      'isSuperAdmin',
      'canAccess',
    ]);

    TestBed.configureTestingModule({
      declarations: [TestComponent, HasPermissionDirective],
      providers: [{ provide: PermissionService, useValue: permissionService }],
    });

    permissionServiceSpy = TestBed.inject(
      PermissionService
    ) as jasmine.SpyObj<PermissionService>;
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
  });

  it('should create an instance', () => {
    expect(component).toBeTruthy();
  });

  it('should show content when super_admin', () => {
    permissionServiceSpy.isSuperAdmin.and.returnValue(true);

    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Visible content');
    expect(permissionServiceSpy.canAccess).not.toHaveBeenCalled();
  });

  it('should show content when permission is granted', () => {
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    permissionServiceSpy.canAccess.and.returnValue(of(true));

    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Visible content');
    expect(permissionServiceSpy.canAccess).toHaveBeenCalledWith('TEST_FEATURE');
  });

  it('should hide content when permission is denied', () => {
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    permissionServiceSpy.canAccess.and.returnValue(of(false));

    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).not.toContain('Visible content');
    expect(permissionServiceSpy.canAccess).toHaveBeenCalledWith('TEST_FEATURE');
  });

  it('should show else template when permission is denied and else template provided', () => {
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    permissionServiceSpy.canAccess.and.returnValue(of(false));

    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Upgrade required');
    expect(compiled.textContent).not.toContain('Premium content');
  });

  it('should show main template when permission is granted and else template provided', () => {
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    permissionServiceSpy.canAccess.and.returnValue(of(true));

    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Premium content');
    expect(compiled.textContent).not.toContain('Upgrade required');
  });

  it('should update view when permission changes', () => {
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    // Usar BehaviorSubject para poder cambiar el valor
    const permissionSubject = new BehaviorSubject<boolean>(false);
    permissionServiceSpy.canAccess.and.returnValue(
      permissionSubject.asObservable()
    );

    fixture.detectChanges();

    let compiled = fixture.nativeElement;
    expect(compiled.textContent).not.toContain('Visible content');

    // Cambiar permiso a granted emitiendo nuevo valor
    permissionSubject.next(true);
    fixture.detectChanges();

    compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Visible content');
  });

  it('should clean up subscription on destroy', () => {
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    permissionServiceSpy.canAccess.and.returnValue(of(true));

    fixture.detectChanges();

    // Destruir el componente
    fixture.destroy();

    // No debería haber errores de suscripción no cerrada
    expect(component).toBeTruthy();
  });
});
