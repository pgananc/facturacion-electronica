import { Component, OnInit } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { environment } from 'src/environments/environment';
import { Token } from '../../_model/auth/token';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit {
  usuario: string[];
  roles: string;
  constructor() {}

  ngOnInit(): void {
    this.obtainData();
  }

  obtainData() {
    const helper = new JwtHelperService();
    let decodedToken = helper.decodeToken(
      sessionStorage.getItem(environment.TOKEN)!
    );
    this.usuario = decodedToken.userName;
    this.roles = decodedToken.roles;
  }
}
