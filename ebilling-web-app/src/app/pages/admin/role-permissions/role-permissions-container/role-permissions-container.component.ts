import { Component } from '@angular/core';

@Component({
  selector: 'app-role-permissions-container',
  templateUrl: './role-permissions-container.component.html',
  styleUrls: ['./role-permissions-container.component.css'],
})
export class RolePermissionsContainerComponent {
  selectedTab = 0; // 0 = Roles, 1 = Planes

  onTabChange(index: number): void {
    this.selectedTab = index;
  }
}
