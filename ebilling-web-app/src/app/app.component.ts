import { Component } from '@angular/core';
import { Menu } from './_model/auth/menu';
import { MenuService } from './_service/auth/menu.service';
import { LoginService } from './_service/auth/login.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  title = 'ebilling-web-app';

  menus: Menu[] = [];

  constructor(
    private menuService: MenuService,
    public loginService: LoginService
  ) {}

  ngOnInit() {
    this.menuService.menuChange.subscribe((data) => {
      this.menus = data;
    });
  }
}
