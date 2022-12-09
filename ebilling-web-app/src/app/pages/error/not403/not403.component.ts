import { Component, OnInit } from '@angular/core';
import { environment } from 'src/environments/environment';
import { JwtHelperService } from '@auth0/angular-jwt';

@Component({
  selector: 'app-not403',
  templateUrl: './not403.component.html',
  styleUrls: ['./not403.component.css'],
})
export class Not403Component implements OnInit {
  userName: string;
  constructor() {}

  ngOnInit(): void {
    let token = sessionStorage.getItem(environment.TOKEN)!;
    const helper = new JwtHelperService();
    let decodedToken = helper.decodeToken(token);
    this.userName = decodedToken.userName;
  }
}
